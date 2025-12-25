package com.lyz.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户反馈实体
 */
@Data
public class UserFeedback {
    private Long id;
    private Long userId;
    private String planId;
    private LocalDate feedbackDate;

    /**
     * RPE 疲劳度评分 (1-5)
     */
    private Integer rating;

    /**
     * 完成率 (0-100)
     */
    private BigDecimal completionRate;

    /**
     * 实际训练时长 (分钟)
     */
    private Integer actualDurationMinutes;

    /**
     * 用户备注
     */
    private String notes;

    /**
     * 正面感受标签 JSON (如 ["SWEATY", "STRENGTH_UP"])
     */
    private String positiveTags;

    /**
     * 负面感受标签 JSON (如 ["TOO_HARD", "BORING"])
     */
    private String negativeTags;

    /**
     * 酸痛部位 JSON (如 ["KNEE", "LOWER_BACK"])
     */
    private String painAreas;

    /**
     * @deprecated 兼容旧版，统一存储所有标签的 JSON
     */
    @Deprecated
    private String emotionTags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
