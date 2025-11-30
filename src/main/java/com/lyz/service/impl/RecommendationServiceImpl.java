package com.lyz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.mapper.UserFeedbackMapper;
import com.lyz.mapper.UserMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.mapper.UserRecommendationMapper;
import com.lyz.model.dto.RecommendationRequestDTO;
import com.lyz.model.dto.ai.DietConstraints;
import com.lyz.model.dto.ai.UserPromptContext;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.entity.User;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // AI 核心组件 (本次重构引入)
    private final ZhipuAiClient zhipuAiClient;
    private final FatigueAnalyzer fatigueAnalyzer;          // Step 1: 状态分析
    private final MedicalContextBuilder medicalContextBuilder; // Step 2: 规则引擎
    private final PromptTemplateManager promptTemplateManager; // Step 3: 模板管理

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request) {
        // 1. 数据准备 (Data Preparation)
        User user = userMapper.selectById(userId);
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
            DietConstraints constraints = medicalContextBuilder.inferConstraints(
                    profile.getExtractedMedicalData(), profile.getGender()
            );

            // Step 3: 决策今日训练重点 (简单的推导逻辑)
            String targetFocus = determineTrainingFocus(userStatus, history);

            // Step 4: 组装 Prompt 上下文
            UserPromptContext context = UserPromptContext.builder()
                    .basicInfo(formatBasicInfo(profile))
                    .goal(profile.getGoal())
                    .preferences(formatPreferences(profile))
                    .userStatus(userStatus)
                    .constraints(constraints)
                    .isFirstTime(isFirstTime)
                    .targetFocus(targetFocus)
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
     * 根据疲劳部位和历史，决定今天练什么
     */
    private String determineTrainingFocus(UserStatus status, List<UserRecommendation> history) {
        // 1. 如果有明显疲劳部位，必须避开
        if (!status.getFatiguedBodyParts().isEmpty()) {
            List<String> avoid = status.getFatiguedBodyParts();
            if (avoid.contains("下肢")) return "上肢力量 或 核心训练";
            if (avoid.contains("上肢") || avoid.contains("胸部")) return "下肢力量 或 有氧恢复";
            return "低强度全身恢复";
        }

        // 2. 如果建议休息
        if (status.isNeedRestDay()) {
            return "主动恢复 (拉伸/瑜伽)";
        }

        // 3. 简单轮动逻辑 (查看昨天练了什么，这里做个简单模拟，实际可解析历史JSON)
        // 默认为全身综合
        return "全身综合训练";
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