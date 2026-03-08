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
import com.lyz.service.ExerciseEnrichService;
import com.lyz.service.RecommendationService;
import com.lyz.service.analysis.FatigueAnalyzer;
import com.lyz.service.builder.MedicalContextBuilder;
import com.lyz.service.builder.PlanBuilder;
import com.lyz.service.component.NutritionCalculator;
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
import java.time.temporal.ChronoUnit;
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
    private final FatigueAnalyzer fatigueAnalyzer; // Step 1: 状态分析
    private final MedicalContextBuilder medicalContextBuilder; // Step 2: 规则引擎
    private final PromptTemplateManager promptTemplateManager; // Step 3: 模板管理
    private final ExerciseEnrichService exerciseEnrichService; // 动作库匹配增强

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private boolean isLateNight() {
        return LocalTime.now().isAfter(LocalTime.of(LATE_NIGHT_HOUR, 0));
    }

    private final PlanBuilder planBuilder;
    private final NutritionCalculator nutritionCalculator;

    @Override
    public List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request) {
        // 1. 数据准备 (Data Preparation)
        // TODO 后续可以存到缓存，若需要
        UserProfile profile = userProfileMapper.getByUserId(userId);
        if (profile == null)
            throw new IllegalStateException("请先完善健康档案");

        List<UserRecommendation> history = queryRecentPlans(userId, HISTORY_DAYS);

        boolean isFirstTime = history.isEmpty();

        // TODO 时间需要更细粒度
        // 如果是首次使用 且 当前时间晚于 20:00，不调用AI，直接建议休息
        if (isFirstTime && isLateNight()) {
            return planBuilder.buildRestPlan();
        }
        // 2. 核心逻辑链 (Core Logic Pipeline)
        try {
            // Step 1: 分析用户当前状态 (疲劳、心态、趋势)
            List<UserFeedback> feedbacks = queryRecentFeedback(userId, HISTORY_DAYS);
            // 确保 feedbacks 是按日期倒序 (最近的在前)
            feedbacks.sort((a, b) -> b.getFeedbackDate().compareTo(a.getFeedbackDate()));

            UserStatus userStatus = fatigueAnalyzer.analyze(feedbacks, profile);

            // Step 1.5: 营养科学计算 (新增)
            NutritionCalculator.NutritionTarget nutritionTarget = nutritionCalculator.calculate(profile);

            // Step 2: 获取医疗建议 (优先使用 DB 缓存)
            // TODO 若无体检数据指标就不需要医疗建议
            HealthConstraints constraints = null;
            String medicalAdviceText = null;
            if (!StringUtils.isBlank(profile.getExtractedMedicalData())) {
                medicalAdviceText = profile.getMedicalAdvicePrompt();

                if (StringUtils.isBlank(medicalAdviceText)) {
                    // 缓存为空，执行动态推导 (降级策略)
                    if (StringUtils.isNotBlank(profile.getExtractedMedicalData())) {
                        constraints = medicalContextBuilder.inferConstraints(
                                profile.getExtractedMedicalData(), profile.getGender());
                        // 如果有风险，生成具体文本；如果无风险，存入一个占位符，避免下次重复计算
                        medicalAdviceText = medicalContextBuilder
                                .generateMedicalAdvicePrompt(profile.getExtractedMedicalData(), profile.getGender());
                    } else {
                        // 无体检数据
                        constraints = new HealthConstraints();
                    }
                }
                if ("HEALTHY_NO_ADVICE".equals(medicalAdviceText)) {
                    medicalAdviceText = "用户体检指标正常，无特殊医学限制。";
                }
            }

            // 🚀 优化后的代码：提取最近 3 天的训练摘要
            List<String> recentHistory = new ArrayList<>();
            // history 已经是按日期倒序排列的
            int lookBackDays = Math.min(history.size(), 3);

            for (int i = 0; i < lookBackDays; i++) {
                UserRecommendation record = history.get(i);
                String summary = parseTrainingSummary(record); // 下面会优化这个解析方法
                // 格式： "2023-12-10: 胸部力量训练 (重点: 胸大肌)"
                recentHistory.add(record.getDate() + ": " + summary);
            }
            // 如果是空（新用户），填一个默认值
            if (recentHistory.isEmpty()) {
                recentHistory.add("无（新用户首次训练）");
            }

            // 4.1 构建精简版 Profile Map
            Map<String, Object> profileMap = new HashMap<>();
            profileMap.put("gender", profile.getGender() == 1 ? "Male" : "Female");
            profileMap.put("age", profile.getAge());
            profileMap.put("height_cm", profile.getHeightCm());
            profileMap.put("weight_kg", profile.getWeightKg());
            profileMap.put("bmi", calculateBmi(profile));
            profileMap.put("goal", profile.getGoal());
            profileMap.put("fitness_level", profile.getFitnessLevel());
            profileMap.put("available_time_min", profile.getAvailableTimePerDay());
            profileMap.put("gym_environment", profile.getTrainingLocation());
            // 训练频率
            profileMap.put("weekly_training_days", profile.getTrainingFrequency());
            // 2. 偏好：决定 AI 的个性化推荐 (如：不做波比跳)
            if (StringUtils.isNotBlank(profile.getSpecialRestrictions())) {
                profileMap.put("user_preferences", profile.getSpecialRestrictions());
            }
            if (StringUtils.isNotBlank(profile.getMedicalHistory())) {
                profileMap.put("injuries_history", profile.getMedicalHistory());
            }
            if (profile.getTargetWeightKg() != null) {
                profileMap.put("target_weight_kg", profile.getTargetWeightKg());
            }

            // 4.2 构建 Medical Map
            Map<String, Object> medicalMap = new HashMap<>();
            medicalMap.put("advice_summary", medicalAdviceText);
            if (constraints != null) {
                medicalMap.put("strict_constraints", constraints.getForbiddenCategories());
                medicalMap.put("risk_warnings", constraints.getRiskWarning());
            }

            // 4.3 构建 Context 对象
            UserPromptContext context = UserPromptContext.builder()
                    .profile(profileMap) // 注入 Map
                    .nutrition(nutritionTarget) // 注入 Step 1.5 算出的对象
                    .currentStatus(userStatus) // 注入 Step 1 的对象
                    .medicalInfo(medicalMap) // 注入 Map
                    .explicitInstruction(userStatus.getAiInstruction())
                    .isFirstTime(isFirstTime)
                    .recentHistory(recentHistory)
                    .build();

            // Step 5: 渲染 Prompt
            String systemPrompt = promptTemplateManager.buildSystemPrompt();
            String userPrompt = promptTemplateManager.buildUserPrompt(context);

            log.info("AI Prompt生成完毕，UserId={},  疲劳度={}", userId, userStatus.getFatigueLevel());

            // 3. 调用 AI (AI Invocation)
            String rawResponse = zhipuAiClient.chat(systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE,
                    DEFAULT_TOP_P);
            log.info("AI 调用成功，UserId={},  结果={}", userId, rawResponse);

            // 4. 解析与持久化 (Parsing & Persistence)
            List<RecommendationPlanVO> plans = parseAndPersist(userId, rawResponse, profile);
            return plans;

        } catch (Exception e) {
            log.error("AI生成失败，启动降级策略: {}", e.getMessage());
            return buildFallbackPlans(profile, e.getMessage());
        }
    }

    // ================= 核心辅助逻辑 =================

    private String parseTrainingSummary(UserRecommendation rec) {
        try {
            // 解析 JSON
            RecommendationPlanVO plan = objectMapper.readValue(rec.getPlanJson(), RecommendationPlanVO.class);
            // 如果是 List 结构，取第一个
            // ... (保留你之前的兼容 List 的逻辑) ...

            // 核心：提取结构化字段，而不是依赖标题
            String title = plan.getTitle();
            String focus = "全身";
            if (plan.getTraining_plan() != null && StringUtils.isNotBlank(plan.getTraining_plan().getFocus_part())) {
                focus = plan.getTraining_plan().getFocus_part();
            }

            return String.format("%s (重点: %s)", title, focus);
        } catch (Exception e) {
            return "未知训练";
        }
    }

    private double calculateBmi(UserProfile p) {
        if (p.getHeightCm() == null || p.getWeightKg() == null)
            return 0;
        double h = p.getHeightCm().doubleValue() / 100.0;
        return p.getWeightKg().doubleValue() / (h * h);
    }

    private List<RecommendationPlanVO> parseAndPersist(Long userId, String rawResponse, UserProfile profile)
            throws JsonProcessingException {
        String jsonPayload = extractJsonBlock(rawResponse);
        List<RecommendationPlanVO> plans = objectMapper.readValue(jsonPayload, new TypeReference<>() {
        });

        if (plans != null && !plans.isEmpty()) {
            RecommendationPlanVO plan = plans.get(0);
            // === 动作库匹配：补充 GIF/要点/肌群 ===
            if (plan.getTraining_plan() != null && plan.getTraining_plan().getMovements() != null) {
                plan.getTraining_plan().setMovements(
                        exerciseEnrichService.enrichMovements(plan.getTraining_plan().getMovements()));
            }
            // === 执行分餐计算逻辑 ===
            calculateMealNutrition(plan.getDiet_plan(), profile);
            // === 持久化 (保存到数据库和 Redis) ===
            persistPlan(userId, plan);
        }
        return plans;
    }

    // ================= 基础设施 =================

    private String extractJsonBlock(String text) {
        if (StringUtils.isBlank(text))
            throw new IllegalStateException("Empty AI response");
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
        RecommendationPlanVO plan = null;
        if (StringUtils.isNotBlank(cached)) {
            try {
                plan = objectMapper.readValue(cached, RecommendationPlanVO.class);
            } catch (Exception ignored) {
            }
        }
        if (plan == null) {
            UserRecommendation rec = userRecommendationMapper.getByUserIdAndDate(userId, LocalDate.now());
            if (rec != null) {
                try {
                    plan = objectMapper.readValue(rec.getPlanJson(), RecommendationPlanVO.class);
                } catch (Exception ignored) {
                }
            }
        }
        // 每次读取时重新 enrich，确保数据库中最新的 gifUrl 能返回给前端
        if (plan != null && plan.getTraining_plan() != null && plan.getTraining_plan().getMovements() != null) {
            plan.getTraining_plan().setMovements(
                    exerciseEnrichService.enrichMovements(plan.getTraining_plan().getMovements()));
        }
        return plan;
    }

    /**
     * 根据总热量，按固定比例分配三餐指标 (早:午:晚 = 30:40:30)
     */
    private void calculateMealNutrition(RecommendationPlanVO.Diet diet, UserProfile profile) {
        if (diet == null || diet.getTotal_calories() == null)
            return;

        // 固定三餐分配比例
        double[] ratios = { 0.30, 0.40, 0.30 }; // 早, 午, 晚

        // 提取总量
        int totalCal = diet.getTotal_calories();
        int totalP = diet.getMacros().getProtein_g();
        int totalC = diet.getMacros().getCarbs_g();
        int totalF = diet.getMacros().getFat_g();

        // 计算并填充三餐对象
        diet.setBreakfast(createMeal("早餐", ratios[0], totalCal, totalP, totalC, totalF));
        diet.setLunch(createMeal("午餐", ratios[1], totalCal, totalP, totalC, totalF));
        diet.setDinner(createMeal("晚餐", ratios[2], totalCal, totalP, totalC, totalF));
    }

    private RecommendationPlanVO.Diet.Meal createMeal(String name, double ratio, int totalCal, int totalP, int totalC,
            int totalF) {
        if (ratio <= 0)
            return null;

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

            // === 智能填充营养记录 ===
            saveNutritionRecordFromPlan(userId, plan);

            // Redis 缓存...
        } catch (Exception e) {
            log.warn("保存计划失败", e);
        }
    }

    /**
     * 从AI生成的计划中提取营养数据并写入nutrition_record表
     * 这样图表API可以直接使用真实数据
     */
    @Autowired
    private com.lyz.mapper.UserNutritionRecordMapper userNutritionRecordMapper;

    private void saveNutritionRecordFromPlan(Long userId, RecommendationPlanVO plan) {
        try {
            RecommendationPlanVO.Diet diet = plan.getDiet_plan();
            if (diet == null || diet.getTotal_calories() == null) {
                return;
            }

            com.lyz.model.entity.UserNutritionRecord record = new com.lyz.model.entity.UserNutritionRecord();
            record.setUserId(userId);
            record.setRecordDate(LocalDate.now());

            // AI推荐的热量同时作为目标值和初始计划摄入值
            java.math.BigDecimal totalCalories = new java.math.BigDecimal(diet.getTotal_calories());
            record.setTargetCalories(totalCalories);
            record.setTotalCalories(totalCalories); // 修复: 确保图表数据正确

            // 三大营养素 (来自AI计划)
            if (diet.getMacros() != null) {
                record.setProtein(new java.math.BigDecimal(diet.getMacros().getProtein_g()));
                record.setCarbohydrate(new java.math.BigDecimal(diet.getMacros().getCarbs_g()));
                record.setFat(new java.math.BigDecimal(diet.getMacros().getFat_g()));
            }

            // 三餐热量 (来自分配计算)
            if (diet.getBreakfast() != null && diet.getBreakfast().getCalories() != null) {
                record.setBreakfastCalories(new java.math.BigDecimal(diet.getBreakfast().getCalories()));
            }
            if (diet.getLunch() != null && diet.getLunch().getCalories() != null) {
                record.setLunchCalories(new java.math.BigDecimal(diet.getLunch().getCalories()));
            }
            if (diet.getDinner() != null && diet.getDinner().getCalories() != null) {
                record.setDinnerCalories(new java.math.BigDecimal(diet.getDinner().getCalories()));
            }

            // 预估消耗热量 (根据训练时长估算)
            RecommendationPlanVO.Training training = plan.getTraining_plan();
            if (training != null && training.getDuration() != null) {
                // 简单估算：每分钟约消耗6-8kcal (取7)
                int durationMinutes = parseDurationMinutes(training.getDuration());
                record.setExerciseDuration(durationMinutes);
                record.setEstimatedBurn(new java.math.BigDecimal(durationMinutes * 7));
            }

            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            userNutritionRecordMapper.insertOrUpdate(record);
            log.info("用户{}营养记录已自动填充: 目标热量={}kcal", userId, diet.getTotal_calories());

        } catch (Exception e) {
            log.warn("营养记录填充失败: {}", e.getMessage());
        }
    }

    /**
     * 解析训练时长字符串为分钟数
     */
    private int parseDurationMinutes(String duration) {
        if (duration == null)
            return 0;
        // 匹配 "30分钟", "45 min", "1小时" 等格式
        try {
            String num = duration.replaceAll("[^0-9]", "");
            int value = Integer.parseInt(num);
            if (duration.contains("小时") || duration.toLowerCase().contains("hour")) {
                return value * 60;
            }
            return value;
        } catch (Exception e) {
            return 30; // 默认30分钟
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