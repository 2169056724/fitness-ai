package com.lyz.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserRecommendation {
    private Long id;
    private Long userId;
    private String planJson;
    private LocalDate date;

    private LocalDateTime createdAt;
}
