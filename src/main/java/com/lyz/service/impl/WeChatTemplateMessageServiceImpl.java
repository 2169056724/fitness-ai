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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 微信模板消息服务实现
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

    /**
     * Access Token 缓存（简单实现，生产环境建议用Redis）
     */
    private String cachedAccessToken;
    private long accessTokenExpireTime;

    @Override
    public boolean sendMealReminderNotification(Long userId, String mealType) {
        try {
            // 0. 防重复检查：今天是否已经发送过该餐次的提醒
            String redisKey = buildReminderKey(userId, mealType);
            Boolean hasSent = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(hasSent)) {
                log.debug("用户 {} 今日已发送过 {} 提醒，跳过", userId, getMealTypeName(mealType));
                return false;
            }

            // 1. 查询用户信息
            User user = userMapper.selectById(userId);
            if (user == null || StringUtils.isBlank(user.getOpenid())) {
                log.warn("用户 {} 不存在或未绑定openid，跳过通知", userId);
                return false;
            }

            // 2. 查询用户健康档案
            UserProfile profile = userProfileMapper.getByUserId(userId);
            if (profile == null) {
                log.warn("用户 {} 没有健康档案，跳过通知", userId);
                return false;
            }

            // 3. 查询当天的推荐计划
            LocalDate today = LocalDate.now();
            UserRecommendation recommendation = userRecommendationMapper.getByUserIdAndDate(userId, today);
            if (recommendation == null || StringUtils.isBlank(recommendation.getPlanJson())) {
                log.warn("用户 {} 今日暂无推荐计划，跳过通知", userId);
                return false;
            }

            // 4. 解析饮食推荐
            RecommendationPlanVO.Diet.Meal meal = parseMealFromPlan(recommendation.getPlanJson(), mealType);
            if (meal == null) {
                log.warn("用户 {} 今日计划中没有 {} 推荐，跳过通知", userId, getMealTypeName(mealType));
                return false;
            }

            // 5. 构造并发送模板消息
            String accessToken = getAccessToken();
            if (StringUtils.isBlank(accessToken)) {
                log.error("获取微信Access Token失败");
                return false;
            }

            boolean success = sendTemplateMessage(user.getOpenid(), mealType, meal, accessToken);
            
            // 6. 发送成功后，记录到Redis（防止重复发送）
            if (success) {
                // 缓存到今天23:59:59，明天自动过期
                long secondsUntilMidnight = Duration.between(
                    LocalTime.now(),
                    LocalTime.of(23, 59, 59)
                ).getSeconds();
                redisTemplate.opsForValue().set(redisKey, "1", Duration.ofSeconds(secondsUntilMidnight));
                log.info("用户 {} {} 提醒发送成功，已记录防重复标记", userId, getMealTypeName(mealType));
            }
            
            return success;

        } catch (Exception e) {
            log.error("发送用餐提醒失败，userId: {}, mealType: {}", userId, mealType, e);
            return false;
        }
    }

    /**
     * 构建防重复Redis Key
     * 格式：meal_reminder:{date}:{userId}:{mealType}
     * 例如：meal_reminder:2025-11-24:1001:breakfast
     */
    private String buildReminderKey(Long userId, String mealType) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return String.format("meal_reminder:%s:%d:%s", date, userId, mealType);
    }

    @Override
    public int sendMealRemindersForAllUsers(String mealType) {
        log.info("开始批量发送 {} 提醒...", getMealTypeName(mealType));

        // 1. 查询所有用户健康档案
        List<UserProfile> allProfiles = userProfileMapper.selectAllProfiles();
        if (allProfiles == null || allProfiles.isEmpty()) {
            log.info("暂无用户健康档案，跳过通知");
            return 0;
        }

        // 2. 当前时间
        LocalTime now = LocalTime.now();
        int successCount = 0;

        // 3. 遍历所有用户
        for (UserProfile profile : allProfiles) {
            try {
                // 获取用户设置的用餐时间
                LocalTime mealTime = getMealTimeFromProfile(profile, mealType);
                if (mealTime == null) {
                    continue; // 用户未设置该餐时间，跳过
                }

                // 判断是否到了用餐时间（提前15分钟提醒）
                LocalTime reminderTime = mealTime.minusMinutes(15);
                if (isTimeToRemind(now, reminderTime)) {
                    boolean success = sendMealReminderNotification(profile.getUserId(), mealType);
                    if (success) {
                        successCount++;
                    }
                    // 每个用户之间间隔200ms，避免请求过快
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                log.error("发送用餐提醒失败，userId: {}", profile.getUserId(), e);
            }
        }

        log.info("批量发送 {} 提醒完成，成功: {} 人", getMealTypeName(mealType), successCount);
        return successCount;
    }

    /**
     * 解析计划中的饮食推荐
     */
    private RecommendationPlanVO.Diet.Meal parseMealFromPlan(String planJson, String mealType) {
        try {
            // planJson 是一个 JSON 数组，取第一个方案
            RecommendationPlanVO[] plans = objectMapper.readValue(planJson, RecommendationPlanVO[].class);
            if (plans == null || plans.length == 0) {
                return null;
            }

            RecommendationPlanVO.Diet diet = plans[0].getDiet_plan();
            if (diet == null) {
                return null;
            }

            switch (mealType.toLowerCase()) {
                case "breakfast":
                    return diet.getBreakfast();
                case "lunch":
                    return diet.getLunch();
                case "dinner":
                    return diet.getDinner();
                case "snack":
                    return diet.getSnack();
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("解析计划JSON失败", e);
            return null;
        }
    }

    /**
     * 发送微信模板消息
     */
    private boolean sendTemplateMessage(String openid, String mealType, 
                                       RecommendationPlanVO.Diet.Meal meal, String accessToken) {
        try {
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

            // 构造模板消息数据
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("touser", openid);
            requestBody.put("template_id", mealReminderTemplateId);
            requestBody.put("page", "pages/daily-plan/index"); // 跳转到当日计划页面
            requestBody.put("miniprogram_state", miniprogramState);

            // 模板数据
            Map<String, Map<String, String>> data = new HashMap<>();
            data.put("thing1", createDataItem(getMealTypeName(mealType))); // 用餐类型
            
            // 优先显示营养素信息，如果没有则显示menu字段（兼容旧数据）
            String nutritionInfo = formatNutritionForTemplate(meal);
            data.put("thing2", createDataItem(nutritionInfo)); // 营养目标
            
            data.put("thing3", createDataItem(meal.getCalories().toString())); // 热量
            data.put("time4", createDataItem(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))); // 日期

            requestBody.put("data", data);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                if (body != null && (int) body.getOrDefault("errcode", -1) == 0) {
                    log.info("发送模板消息成功，openid: {}, mealType: {}", openid, mealType);
                    return true;
                } else {
                    log.error("发送模板消息失败，响应: {}", body);
                    return false;
                }
            } else {
                log.error("发送模板消息失败，HTTP状态码: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("发送模板消息异常", e);
            return false;
        }
    }

    /**
     * 创建模板消息数据项
     */
    private Map<String, String> createDataItem(String value) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        return item;
    }

    /**
     * 格式化菜单文本（微信模板消息限制20字）
     * @deprecated 已改用 formatNutritionForTemplate
     */
    @Deprecated
    private String formatMenuForTemplate(String menu) {
        if (StringUtils.isBlank(menu)) {
            return "暂无推荐";
        }
        // 去除换行符，截取前20字
        String formatted = menu.replaceAll("\\n", "，");
        if (formatted.length() > 20) {
            return formatted.substring(0, 17) + "...";
        }
        return formatted;
    }
    
    /**
     * 格式化营养素信息用于模板消息（微信限制20字）
     * 优先显示简洁的营养素比例，如果没有则回退到menu字段
     */
    private String formatNutritionForTemplate(RecommendationPlanVO.Diet.Meal meal) {
        if (meal == null) {
            return "暂无推荐";
        }
        
        // 尝试从nutrition字段提取简洁信息
        String nutrition = null;
        if (StringUtils.isNotBlank(nutrition)) {
            // 简化显示：提取蛋白质、碳水、脂肪的克数
            // 例如："蛋白质: 30g | 碳水: 50g | 脂肪: 15g" -> "蛋白30g 碳水50g 脂肪15g"
            String simplified = nutrition
                .replaceAll("蛋白质[:：]?\\s*", "蛋")
                .replaceAll("碳水[化合物]*[:：]?\\s*", "碳")
                .replaceAll("脂肪[:：]?\\s*", "脂")
                .replaceAll("[\\|丨]", " ")
                .replaceAll("\\s+", " ")
                .trim();
            
            // 截取前20字
            if (simplified.length() > 20) {
                simplified = simplified.substring(0, 17) + "...";
            }
            
            if (StringUtils.isNotBlank(simplified) && !simplified.equals("...")) {
                return simplified;
            }
        }
        
        // 回退到menu字段（兼容旧数据）
        String menu = null;
        if (StringUtils.isNotBlank(menu) && !menu.contains("请参考营养素比例")) {
            return formatMenuForTemplate(menu);
        }
        
        // 最终兜底：显示"查看详情"
        return "查看计划了解详情";
    }
    
    /**
     * 格式化热量信息
     */
    private String formatCaloriesForTemplate(String calories) {
        if (StringUtils.isBlank(calories)) {
            return "暂无数据";
        }
        
        // 提取数字部分
        String cleaned = calories.replaceAll("[^0-9.]", "").trim();
        if (StringUtils.isBlank(cleaned)) {
            return calories.length() > 10 ? calories.substring(0, 10) : calories;
        }
        
        return cleaned + "千卡";
    }

    /**
     * 获取微信Access Token
     */
    private String getAccessToken() {
        // 检查缓存
        if (StringUtils.isNotBlank(cachedAccessToken) 
                && System.currentTimeMillis() < accessTokenExpireTime) {
            return cachedAccessToken;
        }

        try {
            String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appId, appSecret
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String accessToken = (String) body.get("access_token");
                Integer expiresIn = (Integer) body.getOrDefault("expires_in", 7200);

                if (StringUtils.isNotBlank(accessToken)) {
                    // 缓存Access Token（提前5分钟过期）
                    cachedAccessToken = accessToken;
                    accessTokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                    log.info("成功获取微信Access Token");
                    return accessToken;
                }
            }

            log.error("获取微信Access Token失败，响应: {}", response.getBody());
            return null;

        } catch (Exception e) {
            log.error("获取微信Access Token异常", e);
            return null;
        }
    }

    /**
     * 从健康档案中获取用餐时间
     */
    private LocalTime getMealTimeFromProfile(UserProfile profile, String mealType) {
        try {
            LocalTime time = null;
            switch (mealType.toLowerCase()) {
                case "breakfast":
                    time = profile.getBreakfastTime();
                    break;
                case "lunch":
                    time = profile.getLunchTime();
                    break;
                case "dinner":
                    time = profile.getDinnerTime();
                    break;
                case "snack":
                    time = profile.getSnackTime();
                    break;
            }

            return time;
        } catch (Exception e) {
            log.error("解析用餐时间失败，userId: {}, mealType: {}", profile.getUserId(), mealType, e);
            return null;
        }
    }

    /**
     * 判断是否到了提醒时间
     * 提醒时间前后5分钟内都算
     */
    private boolean isTimeToRemind(LocalTime now, LocalTime reminderTime) {
        LocalTime startTime = reminderTime.minusMinutes(5);
        LocalTime endTime = reminderTime.plusMinutes(5);
        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /**
     * 获取用餐类型中文名称
     */
    private String getMealTypeName(String mealType) {
        switch (mealType.toLowerCase()) {
            case "breakfast":
                return "早餐";
            case "lunch":
                return "午餐";
            case "dinner":
                return "晚餐";
            case "snack":
                return "加餐";
            default:
                return mealType;
        }
    }
}
