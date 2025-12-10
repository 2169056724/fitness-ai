package com.lyz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.mapper.UserFeedbackMapper;
import com.lyz.mapper.UserMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.mapper.UserRecommendationMapper;
import com.lyz.model.dto.RecommendationRequestDTO;
import com.lyz.model.dto.ai.HealthConstraints;
import com.lyz.model.dto.ai.UserPromptContext;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.entity.UserRecommendation;
import com.lyz.model.vo.RecommendationPlanVO;
import com.lyz.service.RecommendationService;
import com.lyz.service.analysis.FatigueAnalyzer;
import com.lyz.service.builder.MedicalContextBuilder;
import com.lyz.service.builder.PlanBuilder;
import com.lyz.service.manager.PromptTemplateManager;
import com.lyz.util.ZhipuAiClient;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 个性化健身饮食推荐实现 (重构版)
 * 采用 状态分析 -> 规则引擎 -> 模板渲染 的三层架构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    // 常量配置
    private static final String DEFAULT_MODEL = "glm-4.5-air";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final double DEFAULT_TOP_P = 0.7;
    private static final String CACHE_KEY_PREFIX = "recommend:plans:";
    private static final int HISTORY_DAYS = 7;
    private static final int LATE_NIGHT_HOUR = 20;

    // 依赖组件
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRecommendationMapper userRecommendationMapper;
    private final UserFeedbackMapper userFeedbackMapper;

    // AI 核心组件
    private final ZhipuAiClient zhipuAiClient;
    private final FatigueAnalyzer fatigueAnalyzer;          // Step 1: 状态分析
    private final MedicalContextBuilder medicalContextBuilder; // Step 2: 规则引擎
    private final PromptTemplateManager promptTemplateManager; // Step 3: 模板管理

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private boolean isLateNight() {
        return LocalTime.now().isAfter(LocalTime.of(LATE_NIGHT_HOUR, 0));
    }

    private final PlanBuilder planBuilder;

    @Override
    public List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request) {
        // 1. 数据准备 (Data Preparation)
        //TODO 后续可以存到缓存，若需要
        UserProfile profile = userProfileMapper.getByUserId(userId);
        if (profile == null) throw new IllegalStateException("请先完善健康档案");

        List<UserRecommendation> history = queryRecentPlans(userId, HISTORY_DAYS);
        boolean isFirstTime = history.isEmpty();

        //TODO 时间需要更细粒度
        // 如果是首次使用 且 当前时间晚于 20:00，不调用AI，直接建议休息
        if (isFirstTime && isLateNight()) {
            return planBuilder.buildRestPlan();
        }
        List<UserFeedback> feedbacks = queryRecentFeedback(userId, HISTORY_DAYS);

        // 2. 核心逻辑链 (Core Logic Pipeline)
        try {
            // Step 1: 分析用户当前状态 (疲劳、心态、趋势)
            UserStatus userStatus = fatigueAnalyzer.analyze(feedbacks);

            // Step 2: 获取医疗建议 (优先使用 DB 缓存)
            //TODO 若无体检数据指标就不需要医疗建议
            HealthConstraints constraints = null;
            String medicalAdviceText=null;
            if(!StringUtils.isBlank(profile.getExtractedMedicalData())){
                medicalAdviceText = profile.getMedicalAdvicePrompt();

                if (StringUtils.isBlank(medicalAdviceText)) {
                    // 缓存为空，执行动态推导 (降级策略)
                    if (StringUtils.isNotBlank(profile.getExtractedMedicalData())) {
                        constraints = medicalContextBuilder.inferConstraints(
                                profile.getExtractedMedicalData(), profile.getGender()
                        );
                        // 如果有风险，生成具体文本；如果无风险，存入一个占位符，避免下次重复计算
                        medicalAdviceText = medicalContextBuilder.generateMedicalAdvicePrompt(profile.getExtractedMedicalData(), profile.getGender());
                        if ("HEALTHY_NO_ADVICE".equals(medicalAdviceText)) {
                            medicalAdviceText = "用户体检指标正常，无特殊医学限制。";
                        }
                    } else {
                        // 无体检数据
                        constraints = new HealthConstraints();
                    }
                }
            }

            // Step 3: 只返回"强制干预指令"，常规情况返回 null
            String targetFocus = determineTrainingFocus(userStatus);

            // 提取昨日训练内容
            //TODO 提取逻辑需优化
            String lastTrainingContent = "无（首次训练）";
            if (!history.isEmpty()) {
                // history.get(0) 是最近的一条（因为SQL是 order by date desc）
                lastTrainingContent = parseLastTrainingSummary(history.get(0));
            }

            // Step 4: 组装 Prompt 上下文
            UserPromptContext context = UserPromptContext.builder()
                    .basicInfo(formatBasicInfo(profile))
                    .goal(profile.getGoal())
                    .preferences(formatPreferences(profile))
                    .userStatus(userStatus)
                    .medicalAdviceText(medicalAdviceText)
                    .constraints(constraints)
                    .isFirstTime(isFirstTime)
                    .targetFocus(targetFocus)
                    .lastTrainingContent(lastTrainingContent)
                    .build();

            // Step 5: 渲染 Prompt
            String systemPrompt = promptTemplateManager.buildSystemPrompt();
            String userPrompt = promptTemplateManager.buildUserPrompt(context);

            log.info("AI Prompt生成完毕，UserId={}, 重点={}, 疲劳度={}", userId, targetFocus, userStatus.getFatigueLevel());

            // 3. 调用 AI (AI Invocation)
            String rawResponse = zhipuAiClient.chat(systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_TOP_P);

            // 4. 解析与持久化 (Parsing & Persistence)
            List<RecommendationPlanVO> plans = parseAndPersist(userId, rawResponse, profile);
            return plans;

        } catch (Exception e) {
            log.error("AI生成失败，启动降级策略: {}", e.getMessage());
            return buildFallbackPlans(profile, e.getMessage());
        }
    }

    // ================= 核心辅助逻辑 =================

    /**
     * 解析昨天的计划，提取关键信息
     */
    private String parseLastTrainingSummary(UserRecommendation rec) {
        try {
            String json = rec.getPlanJson();
            if (StringUtils.isBlank(json)) return "未知";

            // 1. 尝试作为 List 解析
            if (json.trim().startsWith("[")) {
                List<RecommendationPlanVO> plans = objectMapper.readValue(json, new TypeReference<>() {});
                if (!plans.isEmpty()) return formatPlanSummary(plans.get(0));
            }
            // 2. 尝试作为 Object 解析
            else {
                RecommendationPlanVO plan = objectMapper.readValue(json, RecommendationPlanVO.class);
                return formatPlanSummary(plan);
            }
        } catch (Exception e) {
            log.warn("解析历史计划失败: {}", e.getMessage());
        }
        return "未知";
    }

    // 提取一个通用格式化方法
    private String formatPlanSummary(RecommendationPlanVO plan) {
        String focus = (plan.getTraining_plan() != null) ? plan.getTraining_plan().getFocus_part() : "未知";
        return String.format("%s (重点: %s)", plan.getTitle(), focus);
    }

    /**
     *
     * 只负责“安全风控”
     */
    private String determineTrainingFocus(UserStatus status) {
        // 1. 优先处理疲劳部位避让 (强制指令)
        if (!status.getFatiguedBodyParts().isEmpty()) {
            String avoid = String.join("、", status.getFatiguedBodyParts());
            return "严格避开部位：" + avoid; // 这种指令 AI 必须听
        }

        // 2. 强制休息 (强制指令)
        if (status.isNeedRestDay()) {
            return "主动恢复 (瑜伽/拉伸/冥想)";
        }

        // 3. 常规情况：返回 null，表示"无强制要求，请 AI 自由发挥"
        // 之前的 "return '全身综合训练'" 删掉
        return null;
    }

    private String formatBasicInfo(UserProfile p) {
        // 简单计算 BMI/BMR 用于展示
        double bmi = 0.0;
        if (p.getHeightCm() != null && p.getWeightKg() != null) {
            double h = p.getHeightCm().doubleValue() / 100.0;
            bmi = p.getWeightKg().doubleValue() / (h * h);
        }
        return String.format("%s, %d岁, %.1fcm, %.1fkg, BMI:%.1f, 活动量:%s",
                p.getGender() == 1 ? "男" : "女",
                p.getAge(),
                p.getHeightCm(),
                p.getWeightKg(),
                bmi,
                StringUtils.defaultString(p.getActivityLevel(), "中等"));
    }

    private String formatPreferences(UserProfile p) {
        List<String> prefs = new ArrayList<>();
        if (p.getAvailableTimePerDay() != null) prefs.add("时长:" + p.getAvailableTimePerDay() + "分钟");
        if (StringUtils.isNotBlank(p.getTrainingLocation())) prefs.add("场景:" + p.getTrainingLocation());
        if (StringUtils.isNotBlank(p.getSpecialRestrictions())) prefs.add("伤痛/禁忌:" + p.getSpecialRestrictions());
        return String.join("; ", prefs);
    }

    private List<RecommendationPlanVO> parseAndPersist(Long userId, String rawResponse, UserProfile profile) throws JsonProcessingException {
        String jsonPayload = extractJsonBlock(rawResponse);
        List<RecommendationPlanVO> plans = objectMapper.readValue(jsonPayload, new TypeReference<>() {});

        if (plans != null && !plans.isEmpty()) {
            RecommendationPlanVO plan = plans.get(0);
            // === 执行分餐计算逻辑 ===
            calculateMealNutrition(plan.getDiet_plan(), profile);
            // === 持久化 (保存到数据库和 Redis) ===
            persistPlan(userId, plan);
        }
        return plans;
    }

    // ================= 基础设施 =================

    private String extractJsonBlock(String text) {
        if (StringUtils.isBlank(text)) throw new IllegalStateException("Empty AI response");
        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new IllegalStateException("No JSON array found in response");
    }

    private List<UserRecommendation> queryRecentPlans(Long userId, int days) {
        try {
            return userRecommendationMapper.selectByUserIdAndDateRange(
                    userId, LocalDate.now().minusDays(days), LocalDate.now().minusDays(1));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<UserFeedback> queryRecentFeedback(Long userId, int days) {
        try {
            return userFeedbackMapper.selectByUserIdAndDateRange(
                    userId, LocalDate.now().minusDays(days), LocalDate.now().minusDays(1));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public RecommendationPlanVO getTodayPlan(Long userId) {
        // ... 保持原有逻辑，读取Redis/DB ...
        String cacheKey = CACHE_KEY_PREFIX + userId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cached)) {
            try { return objectMapper.readValue(cached, RecommendationPlanVO.class); } catch (Exception ignored) {}
        }
        UserRecommendation rec = userRecommendationMapper.getByUserIdAndDate(userId, LocalDate.now());
        if (rec != null) {
            try { return objectMapper.readValue(rec.getPlanJson(), RecommendationPlanVO.class); } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 根据总热量和配置，分配三餐/四餐的指标
     */
    private void calculateMealNutrition(RecommendationPlanVO.Diet diet, UserProfile profile) {
        if (diet == null || diet.getTotal_calories() == null) return;

        // 1. 确定分配比例
        boolean hasSnack = profile.getSnackTime() != null;
        double[] ratios; // 早, 午, 晚, 加
        if (hasSnack) {
            ratios = new double[]{0.30, 0.40, 0.20, 0.10};
        } else {
            ratios = new double[]{0.30, 0.40, 0.30, 0.0};
        }

        // 2. 提取总量
        int totalCal = diet.getTotal_calories();
        int totalP = diet.getMacros().getProtein_g();
        int totalC = diet.getMacros().getCarbs_g();
        int totalF = diet.getMacros().getFat_g();

        // 3. 计算并填充对象
        diet.setBreakfast(createMeal("早餐", ratios[0], totalCal, totalP, totalC, totalF));
        diet.setLunch(createMeal("午餐", ratios[1], totalCal, totalP, totalC, totalF));
        diet.setDinner(createMeal("晚餐", ratios[2], totalCal, totalP, totalC, totalF));

        if (hasSnack) {
            diet.setSnack(createMeal("加餐", ratios[3], totalCal, totalP, totalC, totalF));
        } else {
            diet.setSnack(null);
        }
    }

    private RecommendationPlanVO.Diet.Meal createMeal(String name, double ratio, int totalCal, int totalP, int totalC, int totalF) {
        if (ratio <= 0) return null;

        RecommendationPlanVO.Diet.Meal meal = new RecommendationPlanVO.Diet.Meal();
        meal.setName(name);
        meal.setCalories((int) (totalCal * ratio));

        RecommendationPlanVO.Diet.Macros mealMacros = new RecommendationPlanVO.Diet.Macros();
        mealMacros.setProtein_g((int) (totalP * ratio));
        mealMacros.setCarbs_g((int) (totalC * ratio));
        mealMacros.setFat_g((int) (totalF * ratio));
        meal.setMacros(mealMacros);

        // 生成简单的建议文案 (前端展示用)
        meal.setSuggestion(String.format("热量约%dkcal，含蛋白%dg", meal.getCalories(), mealMacros.getProtein_g()));

        return meal;
    }

    private void persistPlan(Long userId, RecommendationPlanVO plan) {
        try {
            String planJson = objectMapper.writeValueAsString(plan);

            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(userId);
            rec.setDate(LocalDate.now());
            rec.setPlanJson(planJson);
            rec.setCreatedAt(LocalDateTime.now());
            userRecommendationMapper.insertOrUpdate(rec);

            // Redis 缓存...
        } catch (Exception e) {
            log.warn("保存计划失败", e);
        }
    }


    // ================= 降级兜底 (简化版) =================

    private List<RecommendationPlanVO> buildFallbackPlans(UserProfile profile, String errorMsg) {
        // 这是一个极简的兜底，保证系统不崩
        RecommendationPlanVO plan = new RecommendationPlanVO();
        plan.setTitle("基础恢复训练 (系统维护中)");
        plan.setReason("由于AI服务暂时繁忙 (" + errorMsg + ")，为您推荐基础方案。");

        RecommendationPlanVO.Training t = new RecommendationPlanVO.Training();
        t.setType("徒手");
        t.setDuration("30分钟");
        t.setIntensity("低");
        t.setPrecautions("请注意保持呼吸节奏");
        plan.setTraining_plan(t);

        RecommendationPlanVO.Diet d = new RecommendationPlanVO.Diet();
        d.setTotal_calories(1800);
        d.setAdvice("请保持均衡饮食，多吃蔬菜，避免油腻。");
        plan.setDiet_plan(d);

        return Collections.singletonList(plan);
    }
}