package com.lyz.model.dto.ai;

import com.lyz.service.component.NutritionCalculator;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Prompt 上下文容器
 * 包含生成 Prompt 所需的所有原料，实现"数据准备"与"文本拼接"分离
 */
@Data
@Builder
public class UserPromptContext {
// === 核心数据区 (JSON Root Keys) ===

    // 1. 用户画像 (Map: 包含 age, gender, bmi, goal, activityLevel 等)
    // 替代了原来的 basicInfo, goal, preferences 字符串
    private Map<String, Object> profile;

    // 2. 科学营养指标 (直接使用计算好的对象)
    private NutritionCalculator.NutritionTarget nutrition;

    // 3. 身体状态 (UserStatus 对象)
    private UserStatus currentStatus;

    // 4. 医学约束 (Map 或 对象)
    private Map<String, Object> medicalInfo;

    // 5. 训练上下文
    private List<String> recentHistory;

    // === 场景控制 (不进入 JSON，仅用于 Java 逻辑判断) ===
    private boolean isFirstTime;

    private String explicitInstruction;
}