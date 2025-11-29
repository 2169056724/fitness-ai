package com.lyz.task;

import com.lyz.service.WeChatTemplateMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用餐提醒定时任务
 * 根据用户在健康档案中设置的用餐时间，提前15分钟发送微信模板消息提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "schedule.meal-reminder", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MealReminderTask {

    private final WeChatTemplateMessageService weChatTemplateMessageService;

    /**
     * 早餐提醒（每5分钟检查一次，5:00-12:00期间）
     * 时间窗口说明：
     * - 最早：5:30用餐，提前15分钟 = 5:15提醒
     * - 最晚：11:30用餐，提前15分钟 = 11:15提醒
     * - 覆盖不同地区的用餐习惯
     */
    @Scheduled(cron = "0 */5 5-12 * * ?")
    public void sendBreakfastReminders() {
        log.info("========== 开始发送早餐提醒 ==========");
        try {
            int count = weChatTemplateMessageService.sendMealRemindersForAllUsers("breakfast");
            log.info("早餐提醒发送完成，成功: {} 人", count);
        } catch (Exception e) {
            log.error("早餐提醒任务执行失败", e);
        }
    }

    /**
     * 午餐提醒（每5分钟检查一次，10:00-15:00期间）
     * 时间窗口说明：
     * - 最早：10:30用餐，提前15分钟 = 10:15提醒
     * - 最晚：14:30用餐，提前15分钟 = 14:15提醒
     * - 覆盖早午餐和正常午餐时间
     */
    @Scheduled(cron = "0 */5 10-15 * * ?")
    public void sendLunchReminders() {
        log.info("========== 开始发送午餐提醒 ==========");
        try {
            int count = weChatTemplateMessageService.sendMealRemindersForAllUsers("lunch");
            log.info("午餐提醒发送完成，成功: {} 人", count);
        } catch (Exception e) {
            log.error("午餐提醒任务执行失败", e);
        }
    }

    /**
     * 晚餐提醒（每5分钟检查一次，16:00-21:00期间）
     * 时间窗口说明：
     * - 最早：16:30用餐，提前15分钟 = 16:15提醒
     * - 最晚：20:30用餐，提前15分钟 = 20:15提醒
     * - 覆盖下午茶晚餐和正常晚餐时间
     */
    @Scheduled(cron = "0 */5 16-21 * * ?")
    public void sendDinnerReminders() {
        log.info("========== 开始发送晚餐提醒 ==========");
        try {
            int count = weChatTemplateMessageService.sendMealRemindersForAllUsers("dinner");
            log.info("晚餐提醒发送完成，成功: {} 人", count);
        } catch (Exception e) {
            log.error("晚餐提醒任务执行失败", e);
        }
    }

    /**
     * 加餐提醒（每5分钟检查一次，9:00-22:00期间）
     * 时间窗口说明：
     * - 加餐时间最灵活，可能是上午加餐、下午加餐或晚间加餐
     * - 覆盖9:30-21:30的加餐时间
     * - 提前15分钟提醒
     */
    @Scheduled(cron = "0 */5 9-22 * * ?")
    public void sendSnackReminders() {
        log.info("========== 开始发送加餐提醒 ==========");
        try {
            int count = weChatTemplateMessageService.sendMealRemindersForAllUsers("snack");
            log.info("加餐提醒发送完成，成功: {} 人", count);
        } catch (Exception e) {
            log.error("加餐提醒任务执行失败", e);
        }
    }
}
