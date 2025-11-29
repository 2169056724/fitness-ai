package com.lyz.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserFeedbackVO {

    private String planId;
    private LocalDate feedbackDate;

    private Integer rating; // 1-5
    private BigDecimal completionRate; // 如 85.50
    private String notes;
    private String emotionTags; // 存储原始 JSON 字符串

}
