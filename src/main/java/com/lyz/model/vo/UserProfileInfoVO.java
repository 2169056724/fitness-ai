package com.lyz.model.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalTime;

/**
 * 用户信息VO
 */
@Data
public class UserProfileInfoVO {
    /**
     * 用户ID
     */
    private Long userId;


    /**
     * 性别 0未知 1男 2女
     */
    private Integer gender;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 身高(cm)
     */
    private java.math.BigDecimal heightCm;

    /**
     * 体重(kg)
     */
    private java.math.BigDecimal weightKg;

    /**
     * 目标
     */
    private String goal;

    /**
     * 目标体重(kg)
     */
    private java.math.BigDecimal targetWeightKg;

    /**
     * 训练场景偏好（健身房/居家/户外/不限）
     */
    private String trainingLocation;

    /**
     * 每天可运动时间（分钟）
     */
    private Integer availableTimePerDay;

    /**
     * 运动基础水平（新手/初级/中级/高级）
     */
    private String fitnessLevel;

    /**
     * 每周训练次数
     */
    private Integer trainingFrequency;

    /**
     * 特殊限制/偏好
     */
    private String specialRestrictions;

    /**
     * 病史
     */
    private String medicalHistory;

    /**
     * 体检单Url
     */
    private String medicalReportPath;

    /**
     * 提取的体检数据
     */
    private String extractedMedicalData;

    /**
     * 早餐时间
     */
    private LocalTime breakfastTime;

    /**
     * 午餐时间
     */
    private LocalTime lunchTime;

    /**
     * 晚餐时间
     */
    private LocalTime dinnerTime;

    /**
     * 加餐时间（可选）
     */
    private LocalTime snackTime;

}

