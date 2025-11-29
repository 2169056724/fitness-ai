package com.lyz.service.impl;

import com.lyz.mapper.UserFeedbackMapper;
import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.vo.UserFeedbackVO;
import com.lyz.service.UserFeedbackService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserFeedbackServiceImpl implements UserFeedbackService {
    @Autowired
    private UserFeedbackMapper userFeedbackMapper;


    /**
     * 创建用户反馈
     * @param userId
     * @param userFeedbackDTO
     * @return
     */
    @Override
    public UserFeedbackVO createFeedback(Long userId, UserFeedbackDTO userFeedbackDTO) {
        UserFeedback userFeedback = new UserFeedback();
        BeanUtils.copyProperties(userFeedbackDTO, userFeedback);

        userFeedback.setUserId(userId);
        userFeedback.setCreatedAt(LocalDateTime.now());
        userFeedback.setUpdatedAt(LocalDateTime.now());

        userFeedbackMapper.insert(userFeedback);
        UserFeedbackVO userFeedbackVO = new UserFeedbackVO();
        BeanUtils.copyProperties(userFeedback, userFeedbackVO);
        return userFeedbackVO;
    }
}
