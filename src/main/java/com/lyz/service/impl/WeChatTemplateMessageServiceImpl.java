package com.lyz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.mapper.UserMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.mapper.UserRecommendationMapper;
import com.lyz.model.entity.User;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.entity.UserRecommendation;
import com.lyz.model.vo.RecommendationPlanVO;
import com.lyz.service.WeChatTemplateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 微信模板消息服务实现 (Refactored)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatTemplateMessageServiceImpl implements WeChatTemplateMessageService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRecommendationMapper userRecommendationMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${wechat.appid}")
    private String appId;

    @Value("${wechat.secret}")
    private String appSecret;

    @Value("${wechat.template.meal-reminder-id}")
    private String mealReminderTemplateId;

    @Value("${wechat.template.miniprogram-state:trial}")
    private String miniprogramState;

    // 简单的内存缓存 AccessToken
    private String cachedAccessToken;
    private long accessTokenExpireTime;

    // 注入线程池 (解决 @Async 失效问题，直接手动提交任务)
    @Qualifier("fileParserExecutor")
    private final Executor executor;

    @Override
    public boolean sendMealReminderNotification(Long userId, String mealType) {
        // 1. 计算时间窗口 (Time Window)
        // 逻辑：定时任务每 5 分钟执行一次 (e.g., 12:00, 12:05)
        // 我们要找的是 "15分钟后要吃饭的用户"
        // 比如现在 12:00，我们要找用餐时间在 12:15 ~ 12:20 之间的用户

        LocalTime now = LocalTime.now();
        LocalTime targetStart = now.plusMinutes(15);
        LocalTime targetEnd = targetStart.plusMinutes(5); // 覆盖 5 分钟间隔

        log.info("开始批量扫描 {} 提醒，扫描窗口: {} - {}",
                getMealTypeName(mealType), targetStart, targetEnd);

        // 2. 数据库级筛选 (性能提升点：只查需要发的人)
        List<UserProfile> profiles = userProfileMapper.selectProfilesByMealTime(
                mealType, targetStart, targetEnd
        );

        if (profiles == null || profiles.isEmpty()) {
            return 0;
        }

        int count = 0;
        // 3. 遍历并异步发送
        for (UserProfile profile : profiles) {
            // 使用 executor.execute 替代 this.sendAsync，确保异步生效
            executor.execute(() -> {
                sendMealReminderNotification(profile.getUserId(), mealType);
            });
            count++;
        }

        log.info("触发 {} 提醒任务，共投递 {} 人 (后台异步发送中)",
                getMealTypeName(mealType), count);
        return count;
    }

    /**
     * 批量发送入口
     * 注：此处保持同步循环，具体发送逻辑下沉到 sendTemplateMessage (可进一步优化为线程池)
     */
    @Override
    public int sendMealRemindersForAllUsers(String mealType) {
        log.info("开始批量扫描 {} 提醒...", getMealTypeName(mealType));
        List<UserProfile> profiles = userProfileMapper.selectAllProfiles();
        if (profiles == null || profiles.isEmpty()) return 0;

        LocalTime now = LocalTime.now();
        int count = 0;

        for (UserProfile profile : profiles) {
            LocalTime mealTime = getMealTimeFromProfile(profile, mealType);
            if (mealTime == null) continue;

            // 判定逻辑：设定时间前 15 分钟 (±2分钟宽容度)
            LocalTime reminderTime = mealTime.minusMinutes(15);
            if (isTimeMatch(now, reminderTime)) {
                // 异步发送，防止阻塞主线程太久
                sendAsync(profile.getUserId(), mealType);
                count++;
            }
        }
        return count;
    }

    // ================== 核心逻辑修正区 ==================

    /**
     * Fix: 正确解析 Plan JSON (Object 而非 Array)
     */
    private RecommendationPlanVO.Diet.Meal parseMealFromPlan(String planJson, String mealType) {
        try {
            // 数据库存的是单个对象，不是数组
            RecommendationPlanVO plan = objectMapper.readValue(planJson, RecommendationPlanVO.class);
            if (plan == null || plan.getDiet_plan() == null) return null;

            RecommendationPlanVO.Diet diet = plan.getDiet_plan();
            switch (mealType.toLowerCase()) {
                case "breakfast": return diet.getBreakfast();
                case "lunch": return diet.getLunch();
                case "dinner": return diet.getDinner();
                case "snack": return diet.getSnack();
                default: return null;
            }
        } catch (Exception e) {
            log.error("计划JSON解析失败: {}", e.getMessage());
            return null; // 解析失败视为无计划
        }
    }

    /**
     * Fix: 基于 Macros 结构化数据生成文案
     */
    private String formatNutritionInfo(RecommendationPlanVO.Diet.Meal meal) {
        // 特殊情况：如果是断食/休息日 (热量为0)
        if (meal.getCalories() == null || meal.getCalories() <= 0) {
            return "建议空腹/饮水";
        }

        // 优先显示建议 (Suggestion)
        if (StringUtils.isNotBlank(meal.getSuggestion())) {
            // 微信限制20字，做截断处理
            return StringUtils.abbreviate(meal.getSuggestion(), 20);
        }

        // 兜底：显示三大营养素
        if (meal.getMacros() != null) {
            return String.format("蛋%dg 碳%dg 脂%dg",
                    meal.getMacros().getProtein_g(),
                    meal.getMacros().getCarbs_g(),
                    meal.getMacros().getFat_g());
        }

        return "请查看详情";
    }

    private boolean sendTemplateMessage(String openid, String mealType,
                                        RecommendationPlanVO.Diet.Meal meal, String accessToken) {
        String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", mealReminderTemplateId);
        body.put("page", "pages/daily-plan/index");
        body.put("miniprogram_state", miniprogramState);

        Map<String, Map<String, String>> data = new HashMap<>();
        // 1. 用餐类型
        data.put("thing1", item(getMealTypeName(mealType) + "提醒"));
        // 2. 推荐内容 (核心修正点)
        data.put("thing2", item(formatNutritionInfo(meal)));
        // 3. 热量目标
        String calText = (meal.getCalories() != null && meal.getCalories() > 0)
                ? meal.getCalories() + "千卡"
                : "0千卡";
        data.put("thing3", item(calText));
        // 4. 时间
        data.put("time4", item(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));

        body.put("data", data);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            if (resp.getBody() != null && Integer.valueOf(0).equals(resp.getBody().get("errcode"))) {
                log.info("推送成功 openid={}", openid);
                return true;
            }
            log.error("推送失败 openid={}, resp={}", openid, resp.getBody());
        } catch (Exception e) {
            log.error("推送请求异常", e);
        }
        return false;
    }

    // ================== 辅助工具方法 ==================

    private Map<String, String> item(String value) {
        Map<String, String> map = new HashMap<>();
        map.put("value", value);
        return map;
    }

    private String buildReminderKey(Long userId, String mealType) {
        return "notify:meal:" + LocalDate.now() + ":" + userId + ":" + mealType;
    }

    private boolean isTimeMatch(LocalTime now, LocalTime target) {
        // 时间窗口：目标时间前后 2.5 分钟内 (配合5分钟一次的定时任务)
        // 例如任务 12:00 执行，覆盖 11:57:30 - 12:02:30
        // 这里的逻辑可以根据你的 Cron 表达式频次微调
        if (target == null) return false;
        long diff = Math.abs(Duration.between(now, target).toMinutes());
        return diff <= 2;
    }

    private LocalTime getMealTimeFromProfile(UserProfile p, String type) {
        switch (type.toLowerCase()) {
            case "breakfast": return p.getBreakfastTime();
            case "lunch": return p.getLunchTime();
            case "dinner": return p.getDinnerTime();
            case "snack": return p.getSnackTime();
            default: return null;
        }
    }

    private String getMealTypeName(String type) {
        switch (type.toLowerCase()) {
            case "breakfast": return "早餐";
            case "lunch": return "午餐";
            case "dinner": return "晚餐";
            case "snack": return "加餐";
            default: return type;
        }
    }

    private String getAccessToken() {
        if (StringUtils.isNotBlank(cachedAccessToken) && System.currentTimeMillis() < accessTokenExpireTime) {
            return cachedAccessToken;
        }
        // ... 原有的 AccessToken 获取逻辑保持不变 ...
        // 为了篇幅省略，请保留原有的 getAccessToken 实现代码
        // 建议：生产环境务必将 Token 存入 Redis，因为微信 Token 有生成频率限制
        return fetchNewAccessToken();
    }

    // (保留原有的 fetchNewAccessToken 逻辑)
    private String fetchNewAccessToken() {
        try {
            String url = String.format(
                    "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                    appId, appSecret
            );
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                String token = (String) response.getBody().get("access_token");
                Integer expiresIn = (Integer) response.getBody().get("expires_in");
                this.cachedAccessToken = token;
                this.accessTokenExpireTime = System.currentTimeMillis() + (expiresIn - 200) * 1000L;
                return token;
            }
        } catch (Exception e) {
            log.error("Token fetch failed", e);
        }
        return null;
    }
}