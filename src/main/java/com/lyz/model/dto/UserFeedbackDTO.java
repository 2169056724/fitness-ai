package com.lyz.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserFeedbackDTO {

    private String planId;
    private LocalDate feedbackDate;

    private Integer rating; // 1-5
    private BigDecimal completionRate; // 如 85.50
    private String notes;
    private String emotionTags;

}
