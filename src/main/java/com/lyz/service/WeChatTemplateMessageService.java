package com.lyz.service;

/**
 * 微信模板消息服务接口
 */
public interface WeChatTemplateMessageService {

    /**
     * 发送用餐提醒通知
     *
     * @param userId   用户ID
     * @param mealType 用餐类型（breakfast/lunch/dinner/snack）
     * @return 是否发送成功
     */
    boolean sendMealReminderNotification(Long userId, String mealType);

    /**
     * 发送用餐提醒给所有到时间的用户
     *
     * @param mealType 用餐类型
     * @return 发送成功的用户数量
     */
    int sendMealRemindersForAllUsers(String mealType);
}
