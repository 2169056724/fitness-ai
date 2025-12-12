package com.lyz.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserFeedbackDTO {

    private String planId;
    private LocalDate feedbackDate;

    /**
     * RPE 主观疲劳度评分 (1-5)
     * 1: 非常轻松, 3: 适中, 5: 力竭
     */
    private Integer rating;

    /**
     * 计划完成率 (0-100)
     */
    private BigDecimal completionRate;

    /**
     * 用户备注 (非结构化)
     */
    private String notes;

    /**
     * 原始标签 JSON (存库用，如 ["膝盖痛", "强度大"])
     */
    private String emotionTags;

    /**
     * 辅助字段：前端直接传数组，Controller转 JSON 存库
     * 包含：正面感受(暴汗)、负面感受(太难)、风险部位(膝盖痛)
     */
    private List<String> tagList;
}