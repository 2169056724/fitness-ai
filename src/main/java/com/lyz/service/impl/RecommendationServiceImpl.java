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
import com.lyz.service.manager.PromptTemplateManager;
import com.lyz.util.ZhipuAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Override
    public List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request) {
        // 1. 数据准备 (Data Preparation)
        UserProfile profile = userProfileMapper.getByUserId(userId);
        if (profile == null) throw new IllegalStateException("请先完善健康档案");

        List<UserRecommendation> history = queryRecentPlans(userId, HISTORY_DAYS);
        List<UserFeedback> feedbacks = queryRecentFeedback(userId, HISTORY_DAYS);
        boolean isFirstTime = history.isEmpty();

        // 2. 核心逻辑链 (Core Logic Pipeline)
        try {
            // Step 1: 分析用户当前状态 (疲劳、心态、趋势)
            UserStatus userStatus = fatigueAnalyzer.analyze(feedbacks);

            // Step 2: 推导饮食硬性约束 (痛风、血糖等)
            HealthConstraints constraints = medicalContextBuilder.inferConstraints(
                    profile.getExtractedMedicalData(), profile.getGender()
            );

            // Step 3: 只返回"强制干预指令"，常规情况返回 null
            String targetFocus = determineTrainingFocus(userStatus);

            // 提取昨日训练内容
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
            List<RecommendationPlanVO> plans = parseAndPersist(userId, rawResponse);
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
            // 解析 JSON 数组的第一个方案
            List<RecommendationPlanVO> plans = objectMapper.readValue(
                    rec.getPlanJson(),
                    new TypeReference<List<RecommendationPlanVO>>() {}
            );

            if (plans != null && !plans.isEmpty()) {
                RecommendationPlanVO plan = plans.get(0);
                String title = plan.getTitle();
                String focus = "未知";
                if (plan.getTraining_plan() != null) {
                    focus = plan.getTraining_plan().getFocus_part();
                }
                return String.format("%s (重点部位: %s)", title, focus);
            }
        } catch (Exception e) {
            log.warn("解析历史计划失败", e);
        }
        return "未知";
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

    private List<RecommendationPlanVO> parseAndPersist(Long userId, String rawResponse) throws JsonProcessingException {
        String jsonPayload = extractJsonBlock(rawResponse);
        List<RecommendationPlanVO> plans = objectMapper.readValue(jsonPayload, new TypeReference<>() {});

        if (plans != null && !plans.isEmpty()) {
            RecommendationPlanVO chosen = plans.get(0);
            try {
                String planJson = objectMapper.writeValueAsString(chosen);
                // 落库
                UserRecommendation rec = new UserRecommendation();
                rec.setUserId(userId);
                rec.setDate(LocalDate.now());
                rec.setPlanJson(planJson);
                rec.setCreatedAt(LocalDateTime.now());
                userRecommendationMapper.insertOrUpdate(rec);

                // 缓存 Redis
                String cacheKey = CACHE_KEY_PREFIX + userId + ":" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
                Duration ttl = Duration.between(LocalDateTime.now(), LocalDate.now().atTime(23, 59, 59));
                stringRedisTemplate.opsForValue().set(cacheKey, planJson, ttl);
            } catch (Exception e) {
                log.warn("保存计划失败，但已成功生成", e);
            }
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
        d.setTotal_calories("1800 kcal");
        d.setAdvice("请保持均衡饮食，多吃蔬菜，避免油腻。");
        plan.setDiet_plan(d);

        return Collections.singletonList(plan);
    }
}