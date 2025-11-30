package com.lyz.model.dto.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Prompt 上下文容器
 * 包含生成 Prompt 所需的所有原料，实现"数据准备"与"文本拼接"分离
 */
@Data
@Builder
public class UserPromptContext {
    // === 基础画像 ===
    private String basicInfo;        // e.g. "男, 25岁, BMI 24.5"
    private String goal;             // e.g. "减脂"
    private String preferences;      // e.g. "每周5练, 膝盖不好"

    // === 核心动态状态 (Step 1 & 2 的产物) ===
    private UserStatus userStatus;       // 疲劳度、心态、趋势
    private DietConstraints constraints; // 饮食禁忌、红线

    // === 场景控制 ===
    private boolean isFirstTime;     // 是否首次
    private String targetFocus;      // 今日训练重点 (e.g. "上肢", "全身恢复")
}