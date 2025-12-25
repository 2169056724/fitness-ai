package com.lyz.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户反馈数据传输对象
 * 采用结构化标签分类，便于 AI 精准分析
 */
@Data
public class UserFeedbackDTO {

    /**
     * 关联的计划ID
     */
    private String planId;

    /**
     * 反馈日期
     */
    private LocalDate feedbackDate;

    /**
     * RPE 主观疲劳度评分 (1-5)
     * 1: 非常轻松, 2: 较为轻松, 3: 适中, 4: 比较累, 5: 力竭
     */
    private Integer rating;

    /**
     * 计划完成率 (0-100)
     */
    private BigDecimal completionRate;

    /**
     * 实际训练时长 (分钟)
     * 用于精确计算训练负荷
     */
    private Integer actualDurationMinutes;

    /**
     * 正面感受标签 (枚举代码)
     * 如: ["SWEATY", "STRENGTH_UP", "ENERGIZED"]
     * 
     * @see com.lyz.common.FeedbackTagConstants
     */
    private List<String> positiveTags;

    /**
     * 负面感受标签 (枚举代码)
     * 如: ["TOO_HARD", "BORING"]
     * 
     * @see com.lyz.common.FeedbackTagConstants
     */
    private List<String> negativeTags;

    /**
     * 酸痛/不适部位 (枚举代码)
     * 如: ["KNEE", "LOWER_BACK"]
     * 
     * @see com.lyz.common.FeedbackTagConstants
     */
    private List<String> painAreas;

    /**
     * 用户备注 (自由文本)
     */
    private String notes;

    // ======== 兼容旧版字段 (过渡期保留) ========

    /**
     * @deprecated 使用 positiveTags/negativeTags/painAreas 替代
     */
    @Deprecated
    private List<String> tagList;

    /**
     * @deprecated 由后端自动生成，无需前端传入
     */
    @Deprecated
    private String emotionTags;
}