package com.lyz.service;

import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.vo.UserFeedbackVO;

public interface UserFeedbackService {

    /**
     * 创建用户反馈
     * 
     * @param userId          用户ID
     * @param userFeedbackDTO 反馈数据
     * @return 创建的反馈VO
     */
    UserFeedbackVO createFeedback(Long userId, UserFeedbackDTO userFeedbackDTO);

    /**
     * 获取用户今日反馈
     * 
     * @param userId 用户ID
     * @return 今日反馈VO，不存在返回null
     */
    UserFeedbackVO getTodayFeedback(Long userId);
}
