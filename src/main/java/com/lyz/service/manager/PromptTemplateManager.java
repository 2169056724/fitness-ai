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
            5. "explicit_instruction": 系统生成的强制调整指令（优先级最高）。
            6. "recent_history": 最近3次的训练记录（用于判断循环与恢复）。
            
            【核心原则】
            1. 饮食：必须严格执行 "nutrition" 字段中的数值（误差 ±10%），切勿自行重新估算 BMR。
            2. 训练：根据 "currentStatus" 和 "profile" 生成今日计划。若疲劳度高，必须降阶。
            3. 禁忌：严格遵守 "medicalInfo" 中的 strict_constraints。
            4. 恢复与循环：
               - 检查 "recent_history"。
               - **肌肉恢复原则**：若昨日练了大肌群（胸/背/腿），今日必须避开。
               - **分化循环原则**：根据前几天的记录推导今日部位。例如：前天推(胸)，昨天拉(背)，今日应安排蹲(腿)或休息。
            5. 指令：若 "explicit_instruction" 字段不为空，必须无条件优先满足该指令的要求（即使与 profile 冲突）。
            
           
            
            【输出格式】
            请确保 "training_plan" 对象中包含 "focus_part" 字段，明确注明今日训练的主导肌群（如：胸大肌、背阔肌、股四头肌、全身等），以便系统记录。
            请仅输出标准 JSON 数组（Array），不要包含 Markdown 标记：
            [
              {
                "title": "今日计划标题",
                "reason": "生成该计划的详细理由...",
                "training_plan": {
                    "type": "训练类型(如: 力量训练/有氧/HIIT/休息)",
                    "duration": "预计时长(如: 45分钟)",
                    "intensity": "强度等级(如: 低/中/高)",
                    "focus_part": "主导肌群(如: 胸部/全身/背部)",
                    "movements": ["动作1 (组数x次数)", "动作2 (组数x次数)", "动作3..."],
                    "precautions": "训练注意事项(字符串或数组)"
                },
                "diet_plan": {
                  "total_calories": 0,
                  "macros": { "protein_g": 0, "carbs_g": 0, "fat_g": 0 },
                  "advice": "饮食建议..."
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
            Map<String, Object> root = new HashMap<>();

            // 1. Profile
            root.put("profile", ctx.getProfile());

            // 2. Nutrition (直接传对象)
            root.put("nutrition", ctx.getNutrition());

            // 3. Status
            root.put("currentStatus", ctx.getCurrentStatus());

            // 4. Medical
            root.put("medicalInfo", ctx.getMedicalInfo());

            // 5. Explicit Instruction (新增核心逻辑)
            // 将分析层生成的自然语言指令注入 Prompt
            if (StringUtils.isNotBlank(ctx.getExplicitInstruction())) {
                root.put("explicit_instruction", ctx.getExplicitInstruction());
            }
            root.put("recent_history", ctx.getRecentHistory());

            // 6. Context
            Map<String, Object> contextInfo = new HashMap<>();
            contextInfo.put("is_first_time", ctx.isFirstTime());
            root.put("context", contextInfo);

            // 序列化为 JSON 字符串
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        } catch (JsonProcessingException e) {
            log.error("Prompt JSON 序列化失败", e);
            return "Error building prompt";
        }
    }
}