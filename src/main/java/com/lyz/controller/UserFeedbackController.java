package com.lyz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        if (userFeedbackDTO.getTagList() != null && !userFeedbackDTO.getTagList().isEmpty()) {
            try {
                // 把前端传的数组 ["痛", "累"] 转成 JSON 字符串 "[\"痛\", \"累\"]" 存库
                String jsonTags = new ObjectMapper().writeValueAsString(userFeedbackDTO.getTagList());
                userFeedbackDTO.setEmotionTags(jsonTags);
            } catch (Exception e) {
                log.error("标签解析失败", e);
            }
        }
        UserFeedbackVO userFeedbackVO = userFeedbackService.createFeedback(userId,userFeedbackDTO);
        return Result.success("创建成功", userFeedbackVO);
    }
}
