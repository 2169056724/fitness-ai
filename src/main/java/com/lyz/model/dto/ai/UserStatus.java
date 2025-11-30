package com.lyz.model.dto.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户当前身体与训练状态快照
 * (Step 1 Output)
 */
@Data
public class UserStatus {
    /**
     * 疲劳等级 (NONE/MILD/SEVERE)
     */
    private String fatigueLevel;

    /**
     * 需要避开的疲劳部位（如：下肢、胸部）
     */
    private List<String> fatiguedBodyParts = new ArrayList<>();

    /**
     * 最近表现趋势 (e.g., "连续3天完成率低于60%", "状态火热")
     */
    private String recentTrend;

    /**
     * 建议训练强度调整系数 (e.g., 0.8 表示降阶, 1.2 表示进阶)
     */
    private Double intensityAdjustment;

    /**
     * 心态/情绪摘要
     */
    private String moodSummary;

    /**
     * 是否需要恢复日
     */
    private boolean needRestDay;
}