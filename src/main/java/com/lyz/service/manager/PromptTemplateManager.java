package com.lyz.service.manager;

import com.lyz.model.dto.ai.UserPromptContext;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.dto.ai.DietConstraints;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Prompt 模板管理器
 * 职责：管理 System/User Prompt 模板，负责将 Context 数据渲染成最终字符串
 */
@Component
public class PromptTemplateManager {

    // ================= System Prompt (人设与输出规范) =================

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一名专业的体能训练与营养专家。请根据用户数据生成今日计划。
            
            【核心原则】
            1. 饮食：不提供具体菜单，只计算【每日热量缺口】及【三大营养素克数】，并根据病史列出【今日严格禁忌】。
            2. 训练：仅生成【今日一天】的计划，标题必须体现今日训练重点（如"下肢力量"或"全身恢复"）。
            3. 风控：若用户疲劳度高，强制安排恢复性训练；若有伤痛，避开相关部位。
            
            【输出格式】
            请严格仅输出以下 JSON 格式（不要包含 Markdown 代码块标记）：
            [
              {
                "title": "{训练标题}",
                "reason": "{推荐理由，简述如何根据用户疲劳度/病史做的调整}",
                "training_plan": {
                  "type": "{训练类型}",
                  "duration": "{总时长}",
                  "intensity": "{强度等级}",
                  "focus_part": "{今日重点部位}",
                  "movements": ["动作1 (组数x次数)", "动作2..."],
                  "precautions": "{注意事项}"
                },
                "diet_plan": {
                  "total_calories": "{总热量 kcal}",
                  "macros": {
                    "protein_g": "{蛋白质克数}",
                    "carbs_g": "{碳水克数}",
                    "fat_g": "{脂肪克数}"
                  },
                  "forbidden_categories": ["{禁忌1}", "{禁忌2}"],
                  "advice": "{一句话饮食策略}"
                }
              }
            ]
            """;

    // ================= User Prompt (填空题) =================

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT_TEMPLATE;
    }

    public String buildUserPrompt(UserPromptContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 1. 基础信息区
        sb.append(String.format("【用户画像】\n%s\n目标：%s\n偏好：%s\n",
                ctx.getBasicInfo(), ctx.getGoal(), ctx.getPreferences()));

        // 2. 身体状态区 (Step 1 产物)
        UserStatus status = ctx.getUserStatus();
        sb.append("\n【今日状态 (反馈分析)】\n");
        if (ctx.isFirstTime()) {
            sb.append("首次使用，无历史反馈数据。\n");
        } else {
            sb.append(String.format("- 疲劳等级：%s\n", status.getFatigueLevel()));
            sb.append(String.format("- 趋势：%s\n", status.getRecentTrend()));
            if (!status.getFatiguedBodyParts().isEmpty()) {
                sb.append(String.format("- ⚠️ 酸痛/疲劳部位（必须避开）：%s\n",
                        String.join(", ", status.getFatiguedBodyParts())));
            }
            if (status.isNeedRestDay()) {
                sb.append("- ⚠️ 系统判定：今日建议【强制休息】或【极低强度恢复】。\n");
            }
        }

        // 3. 饮食约束区 (Step 2 产物)
        DietConstraints diet = ctx.getConstraints();
        sb.append("\n【饮食红线 (体检分析)】\n");
        if (!diet.getForbiddenCategories().isEmpty()) {
            sb.append(String.format("- 🚫 绝对禁忌：%s\n",
                    String.join(", ", diet.getForbiddenCategories())));
        }
        if (StringUtils.isNotBlank(diet.getRiskWarning())) {
            sb.append(String.format("- 风险提示：%s\n", diet.getRiskWarning()));
        }
        if (diet.getForbiddenCategories().isEmpty() && StringUtils.isBlank(diet.getRiskWarning())) {
            sb.append("- 无特殊饮食限制，均衡膳食即可。\n");
        }

        // 4. 任务指令
        sb.append("\n【生成任务】\n");
        sb.append("请基于以上信息，生成今日计划。\n");
        if (StringUtils.isNotBlank(ctx.getTargetFocus())) {
            sb.append(String.format("要求：今日训练重点为【%s】。\n", ctx.getTargetFocus()));
        }
        if (status.getIntensityAdjustment() < 1.0) {
            sb.append("要求：用户状态不佳，请适当【降低强度】。\n");
        } else if (status.getIntensityAdjustment() > 1.0) {
            sb.append("要求：用户状态良好，可适当【增加挑战】。\n");
        }

        return sb.toString();
    }
}