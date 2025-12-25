package com.lyz.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户反馈视图对象
 */
@Data
public class UserFeedbackVO {

    private Long id;
    private String planId;
    private LocalDate feedbackDate;

    private Integer rating;
    private BigDecimal completionRate;
    private Integer actualDurationMinutes;
    private String notes;

    /**
     * 正面感受标签列表
     */
    private List<String> positiveTags;

    /**
     * 负面感受标签列表
     */
    private List<String> negativeTags;

    /**
     * 酸痛部位列表
     */
    private List<String> painAreas;
}
