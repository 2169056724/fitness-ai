package com.lyz.service;

import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.vo.UserFeedbackVO;

public interface UserFeedbackService {
    /**
     * 创建用户反馈
     * @param userFeedbackDTO
     * @return
     */
    UserFeedbackVO createFeedback(Long userId, UserFeedbackDTO userFeedbackDTO);
}
