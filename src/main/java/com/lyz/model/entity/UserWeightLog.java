package com.lyz.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户体重日志实体
 */
@Data
public class UserWeightLog {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 体重(kg)
     */
    private BigDecimal weightKg;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
