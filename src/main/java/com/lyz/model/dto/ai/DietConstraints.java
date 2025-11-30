package com.lyz.model.dto.ai;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 饮食约束规则
 * (Step 2 Output)
 */
@Data
public class DietConstraints {
    /**
     * 必须严格禁止的食物类别 (e.g., "海鲜", "高糖饮料")
     */
    private List<String> forbiddenCategories = new ArrayList<>();

    /**
     * 建议增加的营养素/食材 (e.g., "膳食纤维", "深海鱼")
     */
    private List<String> recommendedElements = new ArrayList<>();

    /**
     * 饮食策略标签 (e.g., "低嘌呤饮食", "低GI饮食")
     */
    private List<String> strategyTags = new ArrayList<>();

    /**
     * 风险提示文案 (用于展示给用户或放入Prompt)
     */
    private String riskWarning;
}