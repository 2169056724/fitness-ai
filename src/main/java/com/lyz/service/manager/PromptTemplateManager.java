package com.lyz.service.manager;

import com.lyz.model.dto.ai.UserPromptContext;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.dto.ai.HealthConstraints;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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
            1. 饮食：不提供具体菜单，只计算【每日需摄入的总热量】及【三大营养素克数】，并根据病史列出【今日严格禁忌】。
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
                  "total_calories": "{总热量，数字类型，如 2000}",
                  "macros": {
                    "protein_g": "{蛋白质克数，数字类型，如 150}",
                    "carbs_g": "{碳水克数，数字类型}",
                    "fat_g": "{脂肪克数，数字类型}"
                  },
                  "forbidden_categories": ["{禁忌1}", "{禁忌2}"],
                  "advice": "{如高蛋白低脂，优先摄入粗粮}"
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

        // 2. 身体状态区
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
        if (StringUtils.isNotBlank(status.getLatestNote())) {
            sb.append(String.format("- 📝 用户主观日记（请重点参考）：\"%s\"\n", status.getLatestNote()));
        }

        // 3. 饮食约束区 - 【修改点】
        sb.append("\n【医学风险与禁忌 (体检分析)】\n");

        // 优先使用缓存的文本字符串
        if (StringUtils.isNotBlank(ctx.getMedicalAdviceText())) {
            sb.append(ctx.getMedicalAdviceText()).append("\n");
        }
        // 否则使用结构化对象生成 (兼容旧逻辑或缓存未命中的情况)
        else if (ctx.getConstraints() != null) {
            HealthConstraints diet = ctx.getConstraints();
            // 3.1 饮食禁忌
            if (!diet.getForbiddenCategories().isEmpty()) {
                sb.append(String.format("- 🚫 饮食绝对禁忌：%s\n",
                        String.join(", ", diet.getForbiddenCategories())));
            }
            // 3.2 训练禁忌
            if (!diet.getTrainingRisks().isEmpty()) {
                sb.append(String.format("- ⚠️ 训练安全红线（严格遵守）：%s\n",
                        String.join("; ", diet.getTrainingRisks())));
            }
            // 3.3 综合风险
            if (StringUtils.isNotBlank(diet.getRiskWarning())) {
                sb.append(String.format("- 综合风险提示：%s\n", diet.getRiskWarning()));
            }
            // 无限制
            if (diet.getForbiddenCategories().isEmpty() && diet.getTrainingRisks().isEmpty() && StringUtils.isBlank(diet.getRiskWarning())) {
                sb.append("- 体检指标正常，无特殊医学限制。\n");
            }
        } else {
            sb.append("- 暂无体检数据参考。\n");
        }

        // 3. 训练上下文
        sb.append("\n【训练上下文】\n");
        sb.append(String.format("- 📅 昨天训练内容：%s\n", ctx.getLastTrainingContent()));

        // 4. 生成任务指令 (大幅修改)
        sb.append("\n【生成任务】\n");
        sb.append("请基于以上信息生成今日计划。决策逻辑如下：\n");

        // 逻辑 A：如果有系统强制指令（比如用户受伤了），最高优先级执行
        if (StringUtils.isNotBlank(ctx.getTargetFocus())) {
            sb.append(String.format("❗ 系统强制要求：今日重点必须为【%s】。\n", ctx.getTargetFocus()));
        }
        // 逻辑 B：如果没有强制指令，让 AI 智能决策
        else {
            sb.append("1. 请遵循科学的分化训练原则 (Split Routine)。\n");
            sb.append("2. 根据【昨天训练内容】，避免连续两天训练相同的高强度部位（例如昨天练了胸，今天就不要练推类动作）。\n");
            sb.append("3. 结合用户偏好，设计最合适的今日重点（如：上肢/下肢分化，或推/拉/蹲分化）。\n");
        }
        if (status.getIntensityAdjustment() < 1.0) {
            sb.append("要求：用户状态不佳，请适当【降低强度】。\n");
        } else if (status.getIntensityAdjustment() > 1.0) {
            sb.append("要求：用户状态良好，可适当【增加挑战】。\n");
        }


        return sb.toString();
    }
}