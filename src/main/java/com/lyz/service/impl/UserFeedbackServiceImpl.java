package com.lyz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.mapper.UserRecommendationMapper;
import com.lyz.mapper.UserFeedbackMapper;
import com.lyz.model.dto.UserFeedbackDTO;
import com.lyz.model.entity.UserFeedback;
import com.lyz.model.vo.UserFeedbackVO;
import com.lyz.service.UserFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFeedbackServiceImpl implements UserFeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;
    private final UserRecommendationMapper userRecommendationMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建用户反馈
     */
    @Override
    public UserFeedbackVO createFeedback(Long userId, UserFeedbackDTO dto) {
        UserFeedback entity = new UserFeedback();

        // 基础字段
        entity.setUserId(userId);
        
        String planId = dto.getPlanId();
        if ((planId == null || planId.trim().isEmpty())) {
            com.lyz.model.entity.UserRecommendation rec = userRecommendationMapper.getByUserIdAndDate(userId, LocalDate.now());
            if (rec != null && rec.getId() != null) {
                planId = String.valueOf(rec.getId());
            } else {
                // 如果当天没有计划，使用一个默认值或者处理抛出异常
               planId = "0";
            }
        }
        entity.setPlanId(planId);
        
        entity.setFeedbackDate(dto.getFeedbackDate() != null ? dto.getFeedbackDate() : LocalDate.now());
        entity.setRating(dto.getRating());
        entity.setCompletionRate(dto.getCompletionRate());
        entity.setActualDurationMinutes(dto.getActualDurationMinutes());
        entity.setNotes(dto.getNotes());

        // 结构化标签 -> JSON 存储
        entity.setPositiveTags(toJson(dto.getPositiveTags()));
        entity.setNegativeTags(toJson(dto.getNegativeTags()));
        entity.setPainAreas(toJson(dto.getPainAreas()));

        // 时间戳
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        userFeedbackMapper.insert(entity);

        log.info("用户反馈已保存, userId={}, feedbackId={}", userId, entity.getId());
        return toVO(entity);
    }

    /**
     * 获取用户今日反馈
     */
    @Override
    public UserFeedbackVO getTodayFeedback(Long userId) {
        LocalDate today = LocalDate.now();
        List<UserFeedback> feedbacks = userFeedbackMapper.selectByUserIdAndDateRange(userId, today, today);
        if (feedbacks == null || feedbacks.isEmpty()) {
            return null;
        }
        return toVO(feedbacks.get(0));
    }

    /**
     * Entity -> VO
     */
    private UserFeedbackVO toVO(UserFeedback entity) {
        UserFeedbackVO vo = new UserFeedbackVO();
        vo.setId(entity.getId());
        vo.setPlanId(entity.getPlanId());
        vo.setFeedbackDate(entity.getFeedbackDate());
        vo.setRating(entity.getRating());
        vo.setCompletionRate(entity.getCompletionRate());
        vo.setActualDurationMinutes(entity.getActualDurationMinutes());
        vo.setNotes(entity.getNotes());

        // JSON -> List
        vo.setPositiveTags(fromJson(entity.getPositiveTags()));
        vo.setNegativeTags(fromJson(entity.getNegativeTags()));
        vo.setPainAreas(fromJson(entity.getPainAreas()));

        return vo;
    }

    /**
     * List -> JSON String
     */
    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("序列化标签失败", e);
            return null;
        }
    }

    /**
     * JSON String -> List
     */
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("反序列化标签失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
