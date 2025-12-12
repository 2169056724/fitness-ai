package com.lyz.model.dto.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户状态快照 (重构版)
 */
@Data
public class UserStatus {

    // 策略枚举：REST(休息), RECOVERY(主动恢复/降阶), SUSTAIN(维持), PROGRESS(进阶), EFFICIENCY(提效)
    public enum Strategy {
        REST,           // 强制休息
        AVOIDANCE,      // 避让伤痛
        RECOVERY,       // 降级/恢复
        SUSTAIN,        // 维持当前
        PROGRESS,       // 进阶
        EFFICIENCY      // 提高效率(缩短时间)
    }

    private Strategy strategy;
    private String fatigueLevel; // NONE, MILD, SEVERE

    // 需要生成的 Prompt 指令 (直接给 AI 看的)
    private String aiInstruction;

    // 具体的避让部位
    private List<String> riskBodyParts = new ArrayList<>();

    // 给前端展示的分析文案 (如：检测到膝盖不适，已为您调整为上肢训练)
    private String userMessage;

    private String latestNote;
    // 在 UserStatus 中增加
    private Double acwrValue; // 给前端画图用，展示用户的疲劳趋势
}