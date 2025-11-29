package com.lyz.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日营养记录实体
 */
@Data
public class UserNutritionRecord {
    /**
     * 主键
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 关联的计划ID
     */
    private Long planId;
    
    /**
     * 记录日期
     */
    private LocalDate recordDate;
    
    /**
     * 每日总热量（kcal）
     */
    private BigDecimal totalCalories;
    
    /**
     * 目标热量（kcal）
     */
    private BigDecimal targetCalories;
    
    /**
     * 蛋白质（g）
     */
    private BigDecimal protein;
    
    /**
     * 碳水化合物（g）
     */
    private BigDecimal carbohydrate;
    
    /**
     * 脂肪（g）
     */
    private BigDecimal fat;
    
    /**
     * 早餐热量
     */
    private BigDecimal breakfastCalories;
    
    /**
     * 午餐热量
     */
    private BigDecimal lunchCalories;
    
    /**
     * 晚餐热量
     */
    private BigDecimal dinnerCalories;
    
    /**
     * 加餐热量
     */
    private BigDecimal snackCalories;
    
    /**
     * 运动时长（分钟）
     */
    private Integer exerciseDuration;
    
    /**
     * 预估消耗热量（kcal）
     */
    private BigDecimal estimatedBurn;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
