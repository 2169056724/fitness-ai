package com.lyz.service.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.dto.ai.UserPromptContext;
import com.lyz.model.dto.ai.UserStatus;
import com.lyz.model.dto.ai.HealthConstraints;
import com.lyz.service.component.NutritionCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 模板管理器
 * 职责：管理 System/User Prompt 模板，负责将 Context 数据渲染成最终字符串
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptTemplateManager {
    private final ObjectMapper objectMapper;

    // ================= System Prompt (人设与输出规范) =================

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一名专业的体能训练与营养专家。
            
            【输入协议】
            用户将提供一个 JSON 数据包，包含以下核心字段：
            1. "profile": 用户基础画像（年龄、BMI、目标等）。
            2. "nutrition": 系统经严谨计算的每日营养目标（热量、三大项）。
            3. "currentStatus": 今日身心状态（疲劳度、反馈）。
            4. "medicalInfo": 医学禁忌与建议。
            
            【核心原则】
            1. 饮食：必须严格执行 "nutrition" 字段中的数值（误差 ±10%），切勿自行重新估算 BMR。
            2. 训练：根据 "currentStatus" 和 "profile" 生成今日计划。若疲劳度高，必须降阶。
            3. 禁忌：严格遵守 "medicalInfo" 中的 strict_constraints。
            
            【输出格式】
            请仅输出标准 JSON 数组（Array），不要包含 Markdown 标记：
            [
              {
                "title": "...",
                "reason": "...",
                "training_plan": { ... },
                "diet_plan": {
                  "total_calories": 0,
                  "macros": { "protein_g": 0, "carbs_g": 0, "fat_g": 0 },
                  "advice": "..."
                }
              }
            ]
            """;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT_TEMPLATE;
    }

    /**
     * 构建 User Prompt (JSON 序列化)
     */
    public String buildUserPrompt(UserPromptContext ctx) {
        try {
            // 构造最终发送给 AI 的 JSON 结构
            // 我们可以在这里做一个扁平化处理，或者直接序列化 ctx 的某些字段
            Map<String, Object> root = new HashMap<>();

            // 1. Profile
            root.put("profile", ctx.getProfile());

            // 2. Nutrition (直接传对象)
            root.put("nutrition", ctx.getNutrition());

            // 3. Status
            root.put("currentStatus", ctx.getCurrentStatus());

            // 4. Medical
            root.put("medicalInfo", ctx.getMedicalInfo());

            // 5. Context
            Map<String, Object> contextInfo = new HashMap<>();
            contextInfo.put("is_first_time", ctx.isFirstTime());
            contextInfo.put("last_training", ctx.getLastTraining());
            if (ctx.getTargetFocus() != null) {
                contextInfo.put("system_force_focus", ctx.getTargetFocus());
            }
            root.put("context", contextInfo);

            // 序列化为 JSON 字符串
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        } catch (JsonProcessingException e) {
            log.error("Prompt JSON 序列化失败", e);
            return "Error building prompt";
        }
    }
}