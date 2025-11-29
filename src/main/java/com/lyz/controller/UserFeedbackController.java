package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.vo.UserFeedbackVO;
import com.lyz.service.UserFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@Slf4j
public class UserFeedbackController {


    @Autowired
    private UserFeedbackService userFeedbackService;

    /**
     * 创建用户反馈
     *
     * @param userFeedbackDTO
     * @return
     */
    @PostMapping
    public Result<UserFeedbackVO> createFeedback(@RequestBody UserFeedbackDTO userFeedbackDTO) {
        //获取用户id
        Long userId = UserContext.getUserId();

        log.info("创建用户反馈，参数: {}", userFeedbackDTO);
        UserFeedbackVO userFeedbackVO = userFeedbackService.createFeedback(userId,userFeedbackDTO);
        return Result.success("创建成功", userFeedbackVO);
    }
}
