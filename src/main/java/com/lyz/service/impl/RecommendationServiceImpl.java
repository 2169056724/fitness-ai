package com.lyz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.mapper.UserFeedbackMapper;
import com.lyz.mapper.UserMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.mapper.UserRecommendationMapper;
import com.lyz.model.dto.RecommendationRequestDTO;
import com.lyz.model.entity.User;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.entity.UserRecommendation;
import com.lyz.model.vo.RecommendationPlanVO;
import com.lyz.service.RecommendationService;
import com.lyz.service.builder.MedicalContextBuilder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个性化健身饮食推荐实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String DEFAULT_ACTIVITY_LEVEL = "中等";
    private static final String DEFAULT_MODEL = "glm-4.5-air";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final double DEFAULT_TOP_P = 0.7;
    private static final String CACHE_KEY_PREFIX = "recommend:plans:";
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int HISTORY_DAYS = 7;

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final ZhipuAiClient zhipuAiClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRecommendationMapper userRecommendationMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final MedicalContextBuilder medicalContextBuilder;
    private final com.lyz.mapper.UserNutritionRecordMapper userNutritionRecordMapper;

    // 生成当天日期字符串，作为缓存 key 的日期部分
    private String todayKey() {
        return LocalDate.now().format(DATE_KEY_FORMATTER);
    }

    // 计算距离当日 23:59:59 的剩余时间，用作 Redis TTL
    private Duration getTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().atTime(23, 59, 59);
        if (now.isAfter(midnight)) {
            return Duration.ZERO;
        }
        return Duration.between(now, midnight);
    }
    // 调用大模型生成当日健身与饮食推荐并落库、缓存
    @Override
    public List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request) {
        RecommendationRequestDTO safeRequest = request != null ? request : new RecommendationRequestDTO();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，请重新登录");
        }

        UserProfile profile = userProfileMapper.getByUserId(userId);
        if (profile == null) {
            throw new IllegalStateException("请先完善健康档案，再生成计划");
        }

        List<UserRecommendation> history = queryRecentPlans(userId, HISTORY_DAYS);
        history.sort(Comparator.comparing(UserRecommendation::getDate));
        List<UserFeedback> recentFeedbacks = queryRecentFeedback(userId, HISTORY_DAYS);
        recentFeedbacks.sort(Comparator.comparing(UserFeedback::getFeedbackDate));

        boolean isFirstTime = history.isEmpty();
        if (isFirstTime) {
            log.info("检测到首次使用，userId={}", userId);
            int currentHour = LocalDateTime.now().getHour();
            if (currentHour >= 20) {
                log.info("首次使用时间较晚（{}:00），提示用户休息，userId={}", currentHour, userId);
                return buildLateNightRestResponse();
            }
            log.info("首次使用时间合适（{}:00），只生成训练计划，userId={}", currentHour, userId);
        }

        String rawResponse = null;
        try {
            String systemPrompt = buildSystemPrompt(isFirstTime);
            String userPrompt = buildUserPrompt(profile, safeRequest, history, recentFeedbacks, isFirstTime);

            log.info("开始生成AI推荐，userId={}，prompt长度={}", userId, userPrompt.length());
            rawResponse = zhipuAiClient.chat(systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE, DEFAULT_TOP_P);
            String jsonPayload = extractJsonBlock(rawResponse);
            List<RecommendationPlanVO> plans = objectMapper.readValue(jsonPayload, new TypeReference<List<RecommendationPlanVO>>() {
            });
            log.info("AI推荐生成成功，userId={}, 推荐方案数量={}", userId, plans != null ? plans.size() : 0);

            try {
                if (plans != null && !plans.isEmpty()) {
                    RecommendationPlanVO chosen = plans.get(0);
                    String planJson = objectMapper.writeValueAsString(chosen);

                    try {
                        UserRecommendation rec = new UserRecommendation();
                        rec.setUserId(userId);
                        rec.setDate(LocalDate.now());
                        rec.setPlanJson(planJson);
                        rec.setCreatedAt(LocalDateTime.now());
                        userRecommendationMapper.insertOrUpdate(rec);
//                        saveNutritionRecord(userId, chosen, rec.getId(), profile);
                    } catch (Exception ex) {
                        log.warn("持久化推荐方案到数据库失败，userId={}", userId, ex);
                    }

                    try {
                        String cacheKey = CACHE_KEY_PREFIX + userId + ":" + todayKey();
                        Duration ttl = getTtlUntilMidnight();
                        stringRedisTemplate.opsForValue().set(cacheKey, planJson, ttl);
                        log.debug("推荐方案已缓存到 Redis，userId={}, TTL(秒)={}", userId, ttl.getSeconds());
                    } catch (Exception ex) {
                        log.warn("缓存推荐方案到 Redis 失败，userId={}", userId, ex);
                    }
                }
            } catch (Exception e) {
                log.warn("处理生成计划的持久化/缓存时发生异常，userId={}", userId, e);
            }

            return plans != null ? plans : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("解析AI返回JSON失败，rawResponse={}", rawResponse, e);
            return buildFallbackPlans(profile, safeRequest, e.getMessage(), isFirstTime);
        } catch (IllegalStateException e) {
            log.warn("调用智谱AI失败，使用规则兜底方案。原因：{}", e.getMessage());
            return buildFallbackPlans(profile, safeRequest, e.getMessage(), isFirstTime);
        }
    }

    // 根据是否首次使用构建系统提示词
    private String buildSystemPrompt(boolean isFirstTime) {
        if (isFirstTime) {
            return """
                    你是一名专业的健康与健身教练AI。这是用户首次使用系统。请根据用户的基础信息、健身目标、病史和体检指标，生成一套针对性的训练方案。

                    请严格按照以下要求输出：
                    1. 输出格式为纯JSON数组，只包含1个对象。
                    2. 对象字段：title；training_plan（含type/duration/intensity/precautions）；diet_plan为null；reason说明欢迎语。
                    3. 训练计划仅生成一天，强度适中、循序渐进，包含详细动作说明和注意事项。
                    4. 仅输出JSON数组，不要输出其他文字。
                    """;
        }

        return """
                你是一名专业的健康与健身教练AI。请结合用户基础信息、目标、病史、体检指标、以及过去执行情况与反馈，生成1套健身与饮食方案。

                核心原则：
                1. 饮食不给具体菜谱，只给宏量框架和食材建议。
                2. 结合多源数据（如体检异常对应饮食禁忌）。
                3. 提供SHAP值解释决策依据。
                4. 训练仅生成一天，强度适中、循序渐进，动作说明清晰。

                请严格按下列JSON格式输出（仅JSON）：
                [
                  {
                    "title": "方案标题",
                    "training_plan": { ... },
                    "reason": "简短一句总结",
                    "shap_explanation": [
                       {"factor": "体检(尿酸偏高)", "weight": 0.35, "desc": "限制红肉海鲜", "type": "negative"},
                       {"factor": "昨日反馈(腿酸)", "weight": 0.45, "desc": "取消深蹲动作", "type": "negative"},
                       {"factor": "目标(减脂)", "weight": 0.20, "desc": "设定约500kcal缺口", "type": "positive"}
                    ],
                    "diet_plan": {
                      "total_calories": 1881,
                      "macros_ratio": { "protein": 0.29, "carbs": 0.45, "fat": 0.26 },
                      "meals": [
                        {"type":"breakfast","title":"早餐","calories":470,"nutrition":{"protein_g":34,"carbs_g":52,"fat_g":21},"suggestions":["水煮蛋","全麦面包","脱脂奶"],"note":"补充优质蛋白"},
                        {"type":"lunch","title":"午餐","calories":752,"nutrition":{"protein_g":55,"carbs_g":84,"fat_g":22},"suggestions":["去皮鸡胸肉","糙米饭","西兰花"],"note":"低GI主食"},
                        {"type":"dinner","title":"晚餐","calories":470,"nutrition":{"protein_g":34,"carbs_g":52,"fat_g":21},"suggestions":["清蒸鱼","紫薯","叶菜"],"note":"清淡七分饱"},
                        {"type":"snack","title":"加餐","calories":188,"nutrition":{"protein_g":14,"carbs_g":21,"fat_g":11},"suggestions":["坚果","蓝莓"],"note":"练前补充"}
                      ],
                      "ai_analysis": {
                        "warning_tags": ["尿酸偏高","减脂"],
                        "principles": [
                          {"text":"尿酸控制","detail":"限制高嘌呤食物","source":"体检报告"},
                          {"text":"高蛋白策略","detail":"维持肌肉量","source":"健身目标"}
                        ],
                        "strategy_summary": "今日控制嘌呤并保证训练后碳水补充。"
                      }
                    }
                  }
                ]
                """;
    }
    // 汇总用户画像、请求、历史与反馈信息生成用户提示词
    private String buildUserPrompt(UserProfile profile, RecommendationRequestDTO request, List<UserRecommendation> history,
                                   List<UserFeedback> feedbacks, boolean isFirstTime) {
        RecommendationRequestDTO safeRequest = request != null ? request : new RecommendationRequestDTO();
        StringBuilder sb = new StringBuilder(4000);

        String activityLevel = StringUtils.defaultIfBlank(profile.getActivityLevel(), DEFAULT_ACTIVITY_LEVEL);
        Optional<Double> bmi = calculateBmi(profile.getHeightCm(), profile.getWeightKg());
        Optional<Double> bmr = calculateBmr(profile.getGender(), profile.getAge(), profile.getHeightCm(), profile.getWeightKg());
        double maintenanceCalories = calculateMaintenanceCalories(bmr, activityLevel);

        sb.append("【用户基础信息】").append(System.lineSeparator());
        sb.append("- 年龄: ").append(profile.getAge() != null ? profile.getAge() + "岁" : "未知").append(System.lineSeparator());
        sb.append("- 性别: ").append(resolveGender(profile.getGender())).append(System.lineSeparator());
        sb.append("- 身高: ").append(formatBigDecimal(profile.getHeightCm(), "cm")).append(System.lineSeparator());
        sb.append("- 体重: ").append(formatBigDecimal(profile.getWeightKg(), "kg")).append(System.lineSeparator());
        sb.append("- 日常活动水平: ").append(activityLevel).append(System.lineSeparator());
        sb.append("- BMI: ").append(bmi.map(this::formatDouble).orElse("无法计算")).append(System.lineSeparator());
        sb.append("- BMR: ").append(bmr.map(val -> formatDouble(val) + " kcal").orElse("无法计算")).append(System.lineSeparator());

        sb.append(System.lineSeparator()).append("【健身信息】").append(System.lineSeparator());
        sb.append("- 健身目标: ").append(StringUtils.defaultIfBlank(profile.getGoal(), "减脂")).append(System.lineSeparator());
        if (profile.getTargetWeightKg() != null) {
            sb.append("- 目标体重: ").append(formatBigDecimal(profile.getTargetWeightKg(), "kg"));
            if (profile.getWeightKg() != null) {
                double diff = profile.getTargetWeightKg().doubleValue() - profile.getWeightKg().doubleValue();
                if (diff > 0) sb.append("（需增重").append(formatDouble(Math.abs(diff))).append("kg）");
                else if (diff < 0) sb.append("（需减重").append(formatDouble(Math.abs(diff))).append("kg）");
            }
            sb.append(System.lineSeparator());
        }
        if (StringUtils.isNotBlank(profile.getTrainingLocation())) {
            sb.append("- 训练场景: ").append(profile.getTrainingLocation()).append(System.lineSeparator());
        }
        if (StringUtils.isNotBlank(profile.getFitnessLevel())) {
            sb.append("- 运动基础: ").append(profile.getFitnessLevel()).append(System.lineSeparator());
        }
        if (profile.getAvailableTimePerDay() != null) {
            sb.append("- 每天可运动时长: ").append(profile.getAvailableTimePerDay()).append("分钟（请按此时间安排训练）").append(System.lineSeparator());
        }
        if (profile.getTrainingFrequency() != null) {
            sb.append("- 每周训练次数: ").append(profile.getTrainingFrequency()).append("次");
            if (profile.getTrainingFrequency() >= 5) {
                sb.append("（高频，注意恢复日）");
            } else if (profile.getTrainingFrequency() <= 3) {
                sb.append("（频次较低，每次需充分刺激）");
            }
            sb.append(System.lineSeparator());
        }
        if (StringUtils.isNotBlank(profile.getSpecialRestrictions())) {
            sb.append("- ⚠️ 特殊限制/偏好: ").append(profile.getSpecialRestrictions()).append(System.lineSeparator());
        }

        boolean hasSnackTime = profile.getSnackTime() != null;
        sb.append(System.lineSeparator()).append("【用餐安排】").append(System.lineSeparator());
        sb.append("- 用户").append(hasSnackTime ? "已设置" : "未设置").append("加餐时间").append(System.lineSeparator());
        if (hasSnackTime) {
            sb.append("- 请按 25% / 40% / 25% / 10% 分配热量，并生成 snack 字段").append(System.lineSeparator());
        } else {
            sb.append("- 请按 30% / 40% / 30% 分配热量，不生成 snack 字段").append(System.lineSeparator());
        }
        sb.append(medicalContextBuilder.build(profile));


        if (safeRequest.getWearable() != null) {
            RecommendationRequestDTO.WearableMetrics wearable = safeRequest.getWearable();
            sb.append(System.lineSeparator()).append("【可穿戴设备数据（今日）】 ");
            if (wearable.getSteps() != null) sb.append("步数 ").append(wearable.getSteps()).append("；");
            if (wearable.getAverageHeartRate() != null) sb.append("平均心率 ").append(wearable.getAverageHeartRate()).append("bpm；");
            if (wearable.getSleepHours() != null) sb.append("睡眠 ").append(formatDouble(wearable.getSleepHours())).append("小时");
            sb.append(System.lineSeparator());
        }


        Map<LocalDate, List<UserFeedback>> feedbackByDate = feedbacks != null
                ? feedbacks.stream().collect(Collectors.groupingBy(UserFeedback::getFeedbackDate))
                : Collections.emptyMap();

        sb.append(System.lineSeparator()).append("【最近执行与反馈对应】").append(System.lineSeparator());
        if (!history.isEmpty()) {
            for (UserRecommendation rec : history) {
                sb.append("- ").append(rec.getDate()).append(": ");
                String planJson = rec.getPlanJson();
                if (StringUtils.isBlank(planJson)) {
                    sb.append("无详细记录").append(System.lineSeparator());
                } else {
                    try {
                        RecommendationPlanVO plan = objectMapper.readValue(planJson, RecommendationPlanVO.class);
                        String title = StringUtils.defaultIfBlank(plan.getTitle(), "(未命名方案)");
                        sb.append(title).append(System.lineSeparator());
                        String reason = plan.getReason();
                        if (StringUtils.isNotBlank(reason)) {
                            if (reason.length() > 300) reason = reason.substring(0, 300) + "...";
                            sb.append("  reason: ").append(reason).append(System.lineSeparator());
                        }
                        RecommendationPlanVO.Training t = plan.getTraining_plan();
                        if (t != null) {
                            sb.append("  训练摘要: ")
                                    .append(StringUtils.defaultIfBlank(t.getType(), "未知"))
                                    .append(" | ").append(StringUtils.defaultIfBlank(t.getDuration(), "未知"))
                                    .append(" | ").append(StringUtils.defaultIfBlank(t.getIntensity(), "未知"))
                                    .append(System.lineSeparator());
                        }
                        RecommendationPlanVO.Diet d = plan.getDiet_plan();
                        if (d != null) {
                            StringBuilder dietSb = new StringBuilder();
                            if (d.getBreakfast() != null) dietSb.append("早餐(").append(defaultIfBlank(d.getBreakfast().getCalories())).append("):").append(defaultIfBlank(d.getBreakfast().getMenu()));
                            if (d.getLunch() != null) {
                                if (dietSb.length() > 0) dietSb.append("; ");
                                dietSb.append("午餐(").append(defaultIfBlank(d.getLunch().getCalories())).append("):").append(defaultIfBlank(d.getLunch().getMenu()));
                            }
                            if (d.getDinner() != null) {
                                if (dietSb.length() > 0) dietSb.append("; ");
                                dietSb.append("晚餐(").append(defaultIfBlank(d.getDinner().getCalories())).append("):").append(defaultIfBlank(d.getDinner().getMenu()));
                            }
                            if (d.getSnack() != null) {
                                if (dietSb.length() > 0) dietSb.append("; ");
                                dietSb.append("加餐(").append(defaultIfBlank(d.getSnack().getCalories())).append("):").append(defaultIfBlank(d.getSnack().getMenu()));
                            }
                            String dietSummary = dietSb.toString();
                            if (dietSummary.length() > 500) dietSummary = dietSummary.substring(0, 500) + "...";
                            if (StringUtils.isNotBlank(dietSummary)) {
                                sb.append("  饮食摘要: ").append(dietSummary).append(System.lineSeparator());
                            }
                        }
                    } catch (Exception e) {
                        String detail = planJson.length() > 500 ? planJson.substring(0, 500) + "..." : planJson;
                        sb.append("(无法解析) 详细方案: ").append(detail).append(System.lineSeparator());
                    }
                }

                List<UserFeedback> dailyFeedbacks = feedbackByDate.getOrDefault(rec.getDate(), new ArrayList<>());
                if (dailyFeedbacks.isEmpty()) {
                    sb.append("  反馈: 无").append(System.lineSeparator());
                } else {
                    for (UserFeedback fb : dailyFeedbacks) {
                        sb.append("  反馈日期: ").append(fb.getFeedbackDate()).append(System.lineSeparator());
                        if (fb.getRating() != null) sb.append("    - 整体评分: ").append(fb.getRating()).append(" / 5 分").append(System.lineSeparator());
                        if (fb.getCompletionRate() != null) sb.append("    - 完成度: ").append(fb.getCompletionRate()).append("%").append(System.lineSeparator());
                        List<String> emotionList = parseEmotionTagsToList(fb.getEmotionTags());
                        if (!emotionList.isEmpty()) sb.append("    - 用户感受: ").append(summarizeEmotionTags(emotionList)).append(System.lineSeparator());
                        if (StringUtils.isNotBlank(fb.getNotes())) sb.append("    - 详细备注: ").append(fb.getNotes()).append(System.lineSeparator());
                    }
                }
                sb.append(System.lineSeparator());
            }
        } else {
            sb.append("暂无执行记录").append(System.lineSeparator());
        }

        if (feedbacks != null && !feedbacks.isEmpty()) {
            sb.append("【疲劳累积分析】").append(System.lineSeparator());
            sb.append(analyzeFatigueAccumulation(feedbacks)).append(System.lineSeparator());
        } else {
            sb.append("暂无反馈数据").append(System.lineSeparator());
            sb.append(System.lineSeparator()).append("【无反馈调整策略】").append(System.lineSeparator());
            int days = calculateConsecutiveDaysWithoutFeedback(history);
            if (days == 1) {
                sb.append("- 检测到1天无训练反馈").append(System.lineSeparator());
                sb.append("- 调整策略：维持当前训练强度（保守）").append(System.lineSeparator());
            } else if (days >= 2 && days <= 3) {
                sb.append("- 检测到连续").append(days).append("天无反馈，建议强度下调10-15%").append(System.lineSeparator());
            } else if (days >= 4) {
                sb.append("- ⚠️ 连续").append(days).append("天无反馈，建议强度下调20-30%，安排恢复性训练").append(System.lineSeparator());
            } else {
                sb.append("- 首次生成或数据不足，按标准流程生成").append(System.lineSeparator());
            }
        }

        String fallbackGuardrail = buildFallbackGuardrailForPrompt(profile, bmi, bmr, activityLevel, maintenanceCalories, isFirstTime);
        sb.append(System.lineSeparator()).append(fallbackGuardrail).append(System.lineSeparator());

        sb.append(System.lineSeparator());
        sb.append("【生成任务】").append(System.lineSeparator());
        if (isFirstTime) {
            sb.append("这是用户首次使用系统，请生成一份适合新手的训练计划；diet_plan 必须为 null，仅生成 training_plan；reason 说明欢迎语。");
        } else {
            sb.append("请基于上述信息生成1份个性化推荐方案，并在 reason 中解释调整依据与安全注意事项。");
        }

        return sb.toString();
    }
    // 构造兜底提示内容，确保模型异常时仍输出可用方案
    private String buildFallbackGuardrailForPrompt(UserProfile profile, Optional<Double> bmi, Optional<Double> bmr,
                                                   String activityLevel, double maintenanceCalories, boolean isFirstTime) {
        StringBuilder guardrail = new StringBuilder();
        guardrail.append("【规则兜底基线（用于校准AI输出）}").append(System.lineSeparator());
        guardrail.append("- 活动水平: ").append(activityLevel)
                .append("，BMR: ").append(bmr.map(this::formatDouble).orElse("未知"))
                .append(" kcal，估算维持热量: ").append(formatDouble(maintenanceCalories)).append(" kcal")
                .append(System.lineSeparator());

        RecommendationPlanVO.Training baseTraining = buildFallbackTrainingPlan(activityLevel);
        guardrail.append("- 训练兜底模板: ").append(defaultIfBlank(baseTraining.getType()))
                .append(" | 时长").append(defaultIfBlank(baseTraining.getDuration()))
                .append(" | 强度").append(defaultIfBlank(baseTraining.getIntensity()))
                .append("，要点: ").append(defaultIfBlank(baseTraining.getPrecautions()))
                .append(System.lineSeparator());

        if (isFirstTime) {
            guardrail.append("- 首次使用仅输出训练计划，diet_plan 必须为 null。").append(System.lineSeparator());
            guardrail.append("- reason 需说明这是欢迎使用的训练安排。").append(System.lineSeparator());
            return guardrail.toString();
        }

        RecommendationPlanVO.Diet dietBaseline = buildFallbackDietPlan(profile, maintenanceCalories);
        guardrail.append("- 饮食兜底热量: ").append(formatDouble(maintenanceCalories))
                .append(" kcal，宏量比例默认30/45/25；用餐分配：")
                .append(profile.getSnackTime() != null ? "25%/40%/25%/10%（含加餐）" : "30%/40%/30%（三餐）")
                .append(System.lineSeparator());

        if (dietBaseline.getMacros() != null) {
            RecommendationPlanVO.Diet.Macros macros = dietBaseline.getMacros();
            guardrail.append("  宏量换算(g): 蛋白质").append(defaultIfBlank(macros.getProtein_g()))
                    .append("g，碳水").append(defaultIfBlank(macros.getCarbs_g()))
                    .append("g，脂肪").append(defaultIfBlank(macros.getFat_g()))
                    .append("g").append(System.lineSeparator());
        }

        guardrail.append("- 请在AI生成时优先满足上述兜底结构，再结合反馈做个性化调整。").append(System.lineSeparator());
        guardrail.append("- 兜底依据: ").append(buildFallbackReason(profile, bmi, bmr, activityLevel, ""));

        return guardrail.toString();
    }

    // 深夜首次使用时返回仅含提醒的占位计划
    private List<RecommendationPlanVO> buildLateNightRestResponse() {
        List<RecommendationPlanVO> list = new ArrayList<>();
        RecommendationPlanVO restPlan = new RecommendationPlanVO();
        restPlan.setId(1);
        restPlan.setTitle("今日宜休息");
        restPlan.setTraining_plan(null);
        restPlan.setDiet_plan(null);
        restPlan.setReason("时间较晚了，今日先休息。明天将为您提供完整的训练与饮食计划，保持良好作息对健康很重要。");
        list.add(restPlan);
        return list;
    }

    // 查询指定天数内的历史推荐计划记录
    private List<UserRecommendation> queryRecentPlans(Long userId, int days) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(days - 1);
        try {
            List<UserRecommendation> history = userRecommendationMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
            log.debug("查询用户历史推荐，userId={}, 开始日期={}, 结束日期={}, 记录数={}", userId, startDate, endDate, history.size());
            return history;
        } catch (Exception e) {
            log.warn("查询历史推荐失败，userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    // 查询指定天数内的用户反馈记录
    private List<UserFeedback> queryRecentFeedback(Long userId, int days) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(days - 1);
        try {
            List<UserFeedback> list = userFeedbackMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
            log.debug("查询用户历史反馈，userId={}, 开始日期={}, 结束日期={}, 记录数={}", userId, startDate, endDate, list.size());
            return list;
        } catch (Exception e) {
            log.warn("查询历史反馈失败，userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    // 根据历史反馈计算疲劳累积情况并给出训练建议
    private String analyzeFatigueAccumulation(List<UserFeedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return "暂无足够数据进行疲劳分析";
        }

        double avgCompletionRate = feedbacks.stream().filter(fb -> fb.getCompletionRate() != null)
                .mapToDouble(fb -> fb.getCompletionRate().doubleValue()).average().orElse(100.0);
        double avgRating = feedbacks.stream().filter(fb -> fb.getRating() != null)
                .mapToInt(UserFeedback::getRating).average().orElse(5.0);

        int fatigueCount = 0;
        int totalEmotionTags = 0;
        for (UserFeedback fb : feedbacks) {
            List<String> emotions = parseEmotionTagsToList(fb.getEmotionTags());
            totalEmotionTags += emotions.size();
            for (String emotion : emotions) {
                if (emotion.contains("疲劳") || emotion.contains("累") || emotion.contains("困") || emotion.contains("酸痛") || emotion.contains("无力")) {
                    fatigueCount++;
                }
            }
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("- 最近几天平均完成率: ").append(String.format("%.1f", avgCompletionRate)).append("%").append(System.lineSeparator());
        analysis.append("- 最近几天平均评分: ").append(String.format("%.1f", avgRating)).append(" / 5星").append(System.lineSeparator());
        if (totalEmotionTags > 0) {
            double fatigueRatio = (double) fatigueCount / totalEmotionTags * 100;
            analysis.append("- 疲劳相关反馈占比: ").append(String.format("%.0f", fatigueRatio)).append("%").append(System.lineSeparator());
        }
        analysis.append(System.lineSeparator());

        boolean needRest = false;
        StringBuilder suggestion = new StringBuilder("【AI调整建议】");
        if (avgCompletionRate < 70) { suggestion.append("检测到完成率较低，"); needRest = true; }
        if (avgRating < 3) { suggestion.append("检测到评分偏低，"); needRest = true; }
        if (fatigueCount >= 2 && totalEmotionTags > 0) { suggestion.append("检测到多次疲劳相关反馈，"); needRest = true; }

        if (needRest) {
            suggestion.append("建议降低今日训练强度30-50%，或安排恢复性训练；保证睡眠7-9小时；注意补充优质蛋白与维生素，如持续疲劳可安排完全休息。");
        } else if (avgCompletionRate >= 90 && avgRating >= 4) {
            suggestion.append("状态良好，可适度提升训练强度或时长（增幅不超过20%），尝试新动作，保持节奏。");
        } else {
            suggestion.append("状态正常，保持当前强度与节奏，注意训练后拉伸与补水补碳水。");
        }

        analysis.append(suggestion);
        return analysis.toString();
    }


    // 统计连续未反馈的天数，用于调整训练强度
    private int calculateConsecutiveDaysWithoutFeedback(List<UserRecommendation> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }
        int consecutiveDays = 0;
        LocalDate checkDate = LocalDate.now().minusDays(1);
        Long userId = history.get(0).getUserId();
        Set<LocalDate> feedbackDates = queryRecentFeedback(userId, 7).stream()
                .map(UserFeedback::getFeedbackDate).collect(Collectors.toSet());

        for (int i = 0; i < 7; i++) {
            LocalDate current = checkDate;
            if (feedbackDates.contains(current)) break;
            boolean hasPlan = history.stream().anyMatch(rec -> rec.getDate().equals(current));
            if (hasPlan) {
                consecutiveDays++;
            } else {
                break;
            }
            checkDate = checkDate.minusDays(1);
        }
        return consecutiveDays;
    }
    // 计算 BMI 数值并进行合法性校验
    private Optional<Double> calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) return Optional.empty();
        if (heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();
        double heightMeter = heightCm.doubleValue() / 100.0;
        double bmi = weightKg.doubleValue() / (heightMeter * heightMeter);
        return Optional.of(round(bmi, 1));
    }

    // 按性别、年龄、身高体重计算基础代谢率
    private Optional<Double> calculateBmr(Integer gender, Integer age, BigDecimal heightCm, BigDecimal weightKg) {
        if (gender == null || age == null || heightCm == null || weightKg == null) return Optional.empty();
        if (age <= 0 || heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) return Optional.empty();
        double weight = weightKg.doubleValue();
        double height = heightCm.doubleValue();
        double bmr;
        if (Integer.valueOf(1).equals(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else if (Integer.valueOf(2).equals(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        } else {
            return Optional.empty();
        }
        return Optional.of(round(bmr, 1));
    }

    // 保留指定小数位并四舍五入
    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    // 将 BigDecimal 转为字符串并附加单位
    private String formatBigDecimal(BigDecimal value, String unit) {
        if (value == null) return "未知";
        return value.stripTrailingZeros().toPlainString() + unit;
    }

    // 以一位小数格式化 double 值
    private String formatDouble(double value) {
        return String.format(Locale.CHINA, "%.1f", value);
    }

    // 将性别编码转换为可读文本
    private String resolveGender(Integer gender) {
        if (gender == null) return "未知";
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }


    // 将空白字符串替换为占位符并去掉换行
    private String defaultIfBlank(String s) {
        return StringUtils.isBlank(s) ? "-" : s.replaceAll("[\r\n]+", " ");
    }

    // 将情绪标签的 JSON/分隔串解析为列表
    private List<String> parseEmotionTagsToList(String emotionTagsJson) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isBlank(emotionTagsJson)) return list;
        String trimmed = emotionTagsJson.trim();
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isArray()) {
                for (JsonNode n : node) {
                    if (n.isTextual()) {
                        String t = n.asText().trim();
                        if (!t.isEmpty()) list.add(t);
                    } else {
                        String t = objectMapper.writeValueAsString(n).trim();
                        if (!t.isEmpty()) list.add(t);
                    }
                }
            } else if (node.isTextual()) {
                String txt = node.asText().trim();
                if (!txt.isEmpty()) list.add(txt);
            }
        } catch (Exception e) {
            for (String part : trimmed.split("[,，;；\\s]+")) {
                String t = part.trim();
                if (!t.isEmpty()) list.add(t);
            }
        }
        return list;
    }

    // 汇总情绪标签，生成疲劳与状态提示
    private String summarizeEmotionTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (String t : tags) {
            if (t.contains("疲劳") || t.contains("累")) {
                parts.add("出现疲劳信号，注意减量与恢复");
            } else if (t.contains("酸") || t.contains("痛") || t.contains("疼")) {
                parts.add("有酸痛或疼痛，建议避免高强度动作并关注恢复/就医");
            } else if (t.contains("焦虑") || t.contains("压力")) {
                parts.add("存在压力或焦虑，建议减量并改善睡眠质量");
            } else {
                parts.add("用户情绪：" + t);
            }
        }
        String summary = String.join("，", parts);
        return summary.length() > 300 ? summary.substring(0, 300) + "..." : summary;
    }

    // 从模型原始回复中提取 JSON 片段
    private String extractJsonBlock(String rawResponse) {
        if (StringUtils.isBlank(rawResponse)) throw new IllegalStateException("AI未返回内容");
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.substring(3, trimmed.length() - 3).trim();
        }
        int arrStart = trimmed.indexOf('[');
        int arrEnd = trimmed.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            return trimmed.substring(arrStart, arrEnd + 1);
        }
        int objStart = trimmed.indexOf('{');
        int objEnd = trimmed.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return trimmed.substring(objStart, objEnd + 1);
        }
        throw new IllegalStateException("AI返回内容不是合法的JSON: " + trimmed);
    }

    // AI 异常时构造兜底的训练与饮食方案列表
    private List<RecommendationPlanVO> buildFallbackPlans(UserProfile profile, RecommendationRequestDTO request, String reasonMessage, boolean isFirstTime) {
        List<RecommendationPlanVO> list = new ArrayList<>();
        String activityLevel = StringUtils.defaultIfBlank(profile.getActivityLevel(), DEFAULT_ACTIVITY_LEVEL);
        Optional<Double> bmi = calculateBmi(profile.getHeightCm(), profile.getWeightKg());
        Optional<Double> bmr = calculateBmr(profile.getGender(), profile.getAge(), profile.getHeightCm(), profile.getWeightKg());
        double maintenanceCalories = calculateMaintenanceCalories(bmr, activityLevel);

        if (isFirstTime) {
            RecommendationPlanVO firstTimePlan = new RecommendationPlanVO();
            firstTimePlan.setId(1);
            firstTimePlan.setTitle("新手训练计划");
            firstTimePlan.setTraining_plan(buildFallbackTrainingPlan(activityLevel));
            firstTimePlan.setDiet_plan(null);
            firstTimePlan.setReason("欢迎使用！今天为您提供基础训练计划，明日将根据您的执行情况提供完整的饮食与训练推荐。当前为规则兜底方案。");
            list.add(firstTimePlan);
            return list;
        }

        RecommendationPlanVO p1 = new RecommendationPlanVO();
        p1.setId(1);
        p1.setTitle("保守方案");
        p1.setTraining_plan(buildFallbackTrainingPlan(activityLevel));
        p1.setDiet_plan(buildFallbackDietPlan(profile, maintenanceCalories - 300));
        p1.setReason(buildFallbackReason(profile, bmi, bmr, activityLevel, reasonMessage));

        RecommendationPlanVO p2 = new RecommendationPlanVO();
        p2.setId(2);
        p2.setTitle("平衡方案");
        RecommendationPlanVO.Training t2 = buildFallbackTrainingPlan(activityLevel);
        t2.setIntensity("中高");
        t2.setDuration("60-75分钟");
        p2.setTraining_plan(t2);
        p2.setDiet_plan(buildFallbackDietPlan(profile, maintenanceCalories));
        p2.setReason("结合目标与恢复能力，提供平衡的训练与营养计划。");

        RecommendationPlanVO p3 = new RecommendationPlanVO();
        p3.setId(3);
        p3.setTitle("进阶方案");
        RecommendationPlanVO.Training t3 = buildFallbackTrainingPlan(activityLevel);
        t3.setIntensity("高");
        t3.setDuration("75-90分钟");
        p3.setTraining_plan(t3);
        p3.setDiet_plan(buildFallbackDietPlan(profile, maintenanceCalories + 300));
        p3.setReason("高强度高负荷计划，适合恢复良好、有训练基础的用户。");

        list.add(p1);
        list.add(p2);
        list.add(p3);
        return list;
    }

    // 根据 BMR 与活动水平计算维持体重所需热量
    private double calculateMaintenanceCalories(Optional<Double> bmr, String activityLevel) {
        return bmr.orElse(1500.0) * getActivityFactor(activityLevel);
    }

    // 将活动水平映射为热量系数
    private double getActivityFactor(String activityLevel) {
        return switch (activityLevel) {
            case "久坐" -> 1.2;
            case "轻度" -> 1.375;
            case "重度" -> 1.725;
            case "运动员", "高强度" -> 1.9;
            default -> 1.55;
        };
    }

    // 构建兜底的训练计划模板，按活动水平调整
    private RecommendationPlanVO.Training buildFallbackTrainingPlan(String activityLevel) {
        RecommendationPlanVO.Training training = new RecommendationPlanVO.Training();
        training.setType("有氧+力量混合");
        training.setDuration("60分钟");
        training.setIntensity("中等");
        String description = switch (activityLevel) {
            case "久坐" -> "30分钟快走 + 15分钟核心激活 + 10分钟拉伸";
            case "重度", "运动员", "高强度" -> "15分钟动态热身 + 40分钟间歇力量循环 + 15分钟放松拉伸";
            default -> "10分钟热身 + 30分钟有氧（跑步/单车）+ 20分钟力量训练（全身循环）+ 10分钟拉伸";
        };
        training.setPrecautions(description);
        return training;
    }

    // 构建兜底的饮食计划模板，根据目标热量拆分餐次
    private RecommendationPlanVO.Diet buildFallbackDietPlan(UserProfile profile, double targetCalories) {
        RecommendationPlanVO.Diet diet = new RecommendationPlanVO.Diet();
        boolean hasSnack = profile.getSnackTime() != null;
        double totalProteinG = targetCalories * 0.3 / 4;
        double totalCarbG = targetCalories * 0.45 / 4;
        double totalFatG = targetCalories * 0.25 / 9;

        double breakfastRatio = hasSnack ? 0.25 : 0.30;
        double lunchRatio = 0.40;
        double dinnerRatio = hasSnack ? 0.25 : 0.30;
        double snackRatio = 0.10;

        RecommendationPlanVO.Diet.Meal breakfast = new RecommendationPlanVO.Diet.Meal();
        breakfast.setCalories(String.format(Locale.CHINA, "%.0f", targetCalories * breakfastRatio));
        breakfast.setMenu("查看营养素分布");
        breakfast.setNutrition(String.format(Locale.CHINA, "蛋白质%.0fg 碳水%.0fg 脂肪%.0fg",
                totalProteinG * breakfastRatio, totalCarbG * breakfastRatio, totalFatG * breakfastRatio));

        RecommendationPlanVO.Diet.Meal lunch = new RecommendationPlanVO.Diet.Meal();
        lunch.setCalories(String.format(Locale.CHINA, "%.0f", targetCalories * lunchRatio));
        lunch.setMenu("查看营养素分布");
        lunch.setNutrition(String.format(Locale.CHINA, "蛋白质%.0fg 碳水%.0fg 脂肪%.0fg",
                totalProteinG * lunchRatio, totalCarbG * lunchRatio, totalFatG * lunchRatio));

        RecommendationPlanVO.Diet.Meal dinner = new RecommendationPlanVO.Diet.Meal();
        dinner.setCalories(String.format(Locale.CHINA, "%.0f", targetCalories * dinnerRatio));
        dinner.setMenu("查看营养素分布");
        dinner.setNutrition(String.format(Locale.CHINA, "蛋白质%.0fg 碳水%.0fg 脂肪%.0fg",
                totalProteinG * dinnerRatio, totalCarbG * dinnerRatio, totalFatG * dinnerRatio));

        diet.setBreakfast(breakfast);
        diet.setLunch(lunch);
        diet.setDinner(dinner);
        if (hasSnack) {
            RecommendationPlanVO.Diet.Meal snack = new RecommendationPlanVO.Diet.Meal();
            snack.setCalories(String.format(Locale.CHINA, "%.0f", targetCalories * snackRatio));
            snack.setMenu("查看营养素分布");
            snack.setNutrition(String.format(Locale.CHINA, "蛋白质%.0fg 碳水%.0fg 脂肪%.0fg",
                    totalProteinG * snackRatio, totalCarbG * snackRatio, totalFatG * snackRatio));
            diet.setSnack(snack);
        }

        diet.setTotal_calories(String.format(Locale.CHINA, "%.0f kcal", targetCalories));
        RecommendationPlanVO.Diet.Macros macros = new RecommendationPlanVO.Diet.Macros();
        macros.setProtein_g(String.format(Locale.CHINA, "%.0f", totalProteinG));
        macros.setProtein_percent("30");
        macros.setCarbs_g(String.format(Locale.CHINA, "%.0f", totalCarbG));
        macros.setCarbs_percent("45");
        macros.setFat_g(String.format(Locale.CHINA, "%.0f", totalFatG));
        macros.setFat_percent("25");
        diet.setMacros(macros);

        RecommendationPlanVO.Diet.FoodExchange foodExchange = new RecommendationPlanVO.Diet.FoodExchange();
        foodExchange.setGrains(String.valueOf((int) Math.round(totalCarbG / 90)));
        foodExchange.setVegetables("5");
        foodExchange.setFruits("2");
        foodExchange.setProtein_foods(String.valueOf((int) Math.round(totalProteinG / 20)));
        foodExchange.setSoy_nuts("1");
        foodExchange.setOils(String.valueOf((int) Math.round(totalFatG / 10)));
        diet.setFood_exchange(foodExchange);

        diet.setDietary_strategy("优先选择低GI碳水；蛋白质来源多样化；烹饪以清蒸/水煮为主；每日饮水1500-2000ml");
        return diet;
    }

    // 生成兜底方案的文字说明，解释推荐依据
    private String buildFallbackReason(UserProfile profile, Optional<Double> bmi, Optional<Double> bmr,
                                       String activityLevel, String reasonMessage) {
        StringBuilder sb = new StringBuilder("基于已知数据制定保守计划。");
        bmi.ifPresent(value -> sb.append("BMI=").append(formatDouble(value)).append("；"));
        bmr.ifPresent(value -> sb.append("基础代谢=").append(formatDouble(value)).append("kcal；"));
        sb.append("日常活动水平：").append(activityLevel).append("；");
        if (StringUtils.isNotBlank(profile.getMedicalHistory())) {
            sb.append("考虑病史：").append(profile.getMedicalHistory()).append("；");
        }
        if (StringUtils.isNotBlank(reasonMessage)) {
            sb.append("AI接口不可用，当前为规则兜底方案。");
        }
        return sb.toString();
    }

    // 优先从缓存和数据库读取当日推荐计划
    @Override
    public RecommendationPlanVO getTodayPlan(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        String cacheKey = CACHE_KEY_PREFIX + userId + ":" + todayKey();
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.isNotBlank(cached)) {
                try {
                    return objectMapper.readValue(cached, RecommendationPlanVO.class);
                } catch (Exception e) {
                    try {
                        List<RecommendationPlanVO> list = objectMapper.readValue(cached, new TypeReference<List<RecommendationPlanVO>>() {
                        });
                        if (list != null && !list.isEmpty()) return list.get(0);
                    } catch (Exception ex) {
                        log.debug("解析缓存当日方案失败，userId={}", userId, ex);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("读取 Redis 当日计划失败，userId={}", userId, e);
        }

        try {
            UserRecommendation latest = userRecommendationMapper.selectLatestByUserId(userId);
            if (latest == null || StringUtils.isBlank(latest.getPlanJson())) return null;
            try {
                return objectMapper.readValue(latest.getPlanJson(), RecommendationPlanVO.class);
            } catch (Exception e) {
                log.warn("解析数据库中存储的计划失败，userId={}", userId, e);
                return null;
            }
        } catch (Exception e) {
            log.warn("查询数据库获取当日计划失败，userId={}", userId, e);
            return null;
        }
    }
    // 将推荐中的饮食与运动信息汇总并保存为营养记录
    private void saveNutritionRecord(Long userId, RecommendationPlanVO plan, Long planId, UserProfile profile) {
        try {
            com.lyz.model.entity.UserNutritionRecord nutritionRecord = new com.lyz.model.entity.UserNutritionRecord();
            nutritionRecord.setUserId(userId);
            nutritionRecord.setPlanId(planId);
            nutritionRecord.setRecordDate(LocalDate.now());

            RecommendationPlanVO.Diet diet = plan.getDiet_plan();
            if (diet != null) {
                BigDecimal totalCal = BigDecimal.ZERO;
                if (diet.getBreakfast() != null && diet.getBreakfast().getCalories() != null) {
                    BigDecimal breakfastCal = parseCalories(diet.getBreakfast().getCalories());
                    nutritionRecord.setBreakfastCalories(breakfastCal);
                    totalCal = totalCal.add(breakfastCal);
                }
                if (diet.getLunch() != null && diet.getLunch().getCalories() != null) {
                    BigDecimal lunchCal = parseCalories(diet.getLunch().getCalories());
                    nutritionRecord.setLunchCalories(lunchCal);
                    totalCal = totalCal.add(lunchCal);
                }
                if (diet.getDinner() != null && diet.getDinner().getCalories() != null) {
                    BigDecimal dinnerCal = parseCalories(diet.getDinner().getCalories());
                    nutritionRecord.setDinnerCalories(dinnerCal);
                    totalCal = totalCal.add(dinnerCal);
                }
                if (diet.getSnack() != null && diet.getSnack().getCalories() != null) {
                    BigDecimal snackCal = parseCalories(diet.getSnack().getCalories());
                    nutritionRecord.setSnackCalories(snackCal);
                    totalCal = totalCal.add(snackCal);
                }
                nutritionRecord.setTotalCalories(totalCal);

                String nutritionStr = null;
                if (diet.getBreakfast() != null && diet.getBreakfast().getNutrition() != null) {
                    nutritionStr = diet.getBreakfast().getNutrition();
                } else if (diet.getLunch() != null && diet.getLunch().getNutrition() != null) {
                    nutritionStr = diet.getLunch().getNutrition();
                }
                if (nutritionStr != null) {
                    parseNutritionMacros(nutritionStr, nutritionRecord);
                }
            }

            Optional<Double> bmr = calculateBmr(profile.getGender(), profile.getAge(), profile.getHeightCm(), profile.getWeightKg());
            if (bmr.isPresent()) {
                String activityLevel = StringUtils.defaultIfBlank(profile.getActivityLevel(), DEFAULT_ACTIVITY_LEVEL);
                double targetCal = calculateMaintenanceCalories(bmr, activityLevel);
                nutritionRecord.setTargetCalories(BigDecimal.valueOf(targetCal).setScale(2, RoundingMode.HALF_UP));
            }

            RecommendationPlanVO.Training training = plan.getTraining_plan();
            if (training != null && training.getDuration() != null) {
                Integer duration = parseDuration(training.getDuration());
                nutritionRecord.setExerciseDuration(duration);
                if (duration != null && duration > 0) {
                    double burnRate = "高".equals(training.getIntensity()) ? 10.0 : ("低".equals(training.getIntensity()) ? 6.0 : 8.0);
                    BigDecimal estimatedBurn = BigDecimal.valueOf(duration * burnRate).setScale(2, RoundingMode.HALF_UP);
                    nutritionRecord.setEstimatedBurn(estimatedBurn);
                }
            }

            nutritionRecord.setCreatedAt(LocalDateTime.now());
            nutritionRecord.setUpdatedAt(LocalDateTime.now());

            userNutritionRecordMapper.insertOrUpdate(nutritionRecord);
            log.info("营养数据已保存，userId={}, date={}", userId, LocalDate.now());
        } catch (Exception e) {
            log.warn("保存营养数据失败，userId={}", userId, e);
        }
    }

    // 从字符串中解析出热量数值
    private BigDecimal parseCalories(String caloriesStr) {
        if (caloriesStr == null) return BigDecimal.ZERO;
        String cleaned = caloriesStr.replaceAll("[^0-9.]", "").trim();
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // 解析营养素字符串，填充碳水/蛋白/脂肪数值
    private void parseNutritionMacros(String nutritionStr, com.lyz.model.entity.UserNutritionRecord record) {
        if (nutritionStr == null) return;
        java.util.regex.Matcher carbMatcher = java.util.regex.Pattern.compile("碳水[化合物]*[\\s:：]*([0-9.]+)\\s*g").matcher(nutritionStr);
        if (carbMatcher.find()) record.setCarbohydrate(new BigDecimal(carbMatcher.group(1)));
        java.util.regex.Matcher proteinMatcher = java.util.regex.Pattern.compile("蛋白质[\\s:：]*([0-9.]+)\\s*g").matcher(nutritionStr);
        if (proteinMatcher.find()) record.setProtein(new BigDecimal(proteinMatcher.group(1)));
        java.util.regex.Matcher fatMatcher = java.util.regex.Pattern.compile("脂肪[\\s:：]*([0-9.]+)\\s*g").matcher(nutritionStr);
        if (fatMatcher.find()) record.setFat(new BigDecimal(fatMatcher.group(1)));
    }

    // 将时长描述解析为分钟（区间取平均）
    private Integer parseDuration(String durationStr) {
        if (durationStr == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+)(?:-([0-9]+))?").matcher(durationStr);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) {
                int max = Integer.parseInt(matcher.group(2));
                return (min + max) / 2;
            }
            return min;
        }
        return null;
    }
}
