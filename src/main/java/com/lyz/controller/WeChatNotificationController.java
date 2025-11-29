package com.lyz.controller;

import com.lyz.service.WeChatTemplateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信通知控制器（测试用）
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat/notification")
@RequiredArgsConstructor
public class WeChatNotificationController {

    private final WeChatTemplateMessageService weChatTemplateMessageService;

    /**
     * 测试发送单个用户的用餐提醒
     *
     * @param userId   用户ID
     * @param mealType 用餐类型（breakfast/lunch/dinner/snack）
     * @return 响应结果
     */
    @PostMapping("/test/send")
    public Map<String, Object> testSendMealReminder(
            @RequestParam Long userId,
            @RequestParam String mealType) {
        
        log.info("收到测试发送请求，userId: {}, mealType: {}", userId, mealType);
        
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = weChatTemplateMessageService.sendMealReminderNotification(userId, mealType);
            result.put("success", success);
            result.put("message", success ? "发送成功" : "发送失败");
            result.put("userId", userId);
            result.put("mealType", mealType);
        } catch (Exception e) {
            log.error("测试发送失败", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试批量发送某餐的提醒
     *
     * @param mealType 用餐类型（breakfast/lunch/dinner/snack）
     * @return 响应结果
     */
    @PostMapping("/test/send-batch")
    public Map<String, Object> testSendBatchMealReminders(@RequestParam String mealType) {
        log.info("收到批量测试发送请求，mealType: {}", mealType);
        
        Map<String, Object> result = new HashMap<>();
        try {
            int successCount = weChatTemplateMessageService.sendMealRemindersForAllUsers(mealType);
            result.put("success", true);
            result.put("message", "批量发送完成");
            result.put("mealType", mealType);
            result.put("successCount", successCount);
        } catch (Exception e) {
            log.error("批量测试发送失败", e);
            result.put("success", false);
            result.put("message", "发送异常: " + e.getMessage());
        }
        
        return result;
    }
}
