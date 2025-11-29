package com.lyz.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class UserProfile {
    /**
     * 主键
     */
    private Long id;

    /**
     * 关联用户id
     */
    private Long userId;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 性别 0未知 1男 2女
     */
    private Integer gender;

    /**
     * 身高(cm)
     */
    private BigDecimal heightCm;

    /**
     * 体重(kg)
     */
    private BigDecimal weightKg;

    /**
     * 健身目标（减脂/增肌/塑形/健康维护）
     */
    private String goal;

    /**
     * 目标体重(kg) - 用于减脂/增肌进度跟踪
     */
    private BigDecimal targetWeightKg;

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
     * 特殊限制/偏好（如：膝盖不好、腰椎问题、不喜欢跳跃等）
     */
    private String specialRestrictions;

    /**
     * 病史
     */
    private String medicalHistory;

    /**
     * 体检单路径
     */
    private String medicalReportPath;

    /**
     * 识别信息
     */
    private String extractedMedicalData;
    /**
     * 日常活动水平，例如：久坐、轻度、中等、重度、运动员
     */
    private String activityLevel;

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

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 预计算的医疗建议提示词
     */
    private String medicalAdvicePrompt;

}
