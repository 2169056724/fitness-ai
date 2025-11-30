package com.lyz.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 生成的健身与饮食推荐结果
 * 对应 PromptTemplateManager 中的 JSON 输出格式
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationPlanVO {

    private String title;
    private String reason;

    private Training training_plan;
    private Diet diet_plan;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Training {
        private String type;        // 训练类型
        private String duration;    // 时长
        private String intensity;   // 强度
        private String focus_part;  // 重点部位 (Prompt新加字段)

        // 动作列表 (Prompt输出的是数组)
        private List<String> movements;

        // 注意事项 (兼容字符串或数组)
        @JsonDeserialize(using = PrecautionsDeserializer.class)
        private String precautions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Diet {
        // === 核心新字段 (对应 Prompt) ===
        private String total_calories;      // 总热量
        private Macros macros;              // 宏量营养素
        private List<String> forbidden_categories; // 今日禁忌
        private String advice;              // 饮食建议/策略

        // === 兼容旧字段 (保留以防止 WeChatTemplateMessageService 报错) ===
        // 如果 AI 没生成这些，它们就是 null，通知服务会跳过发送，不会崩
        private Meal breakfast;
        private Meal lunch;
        private Meal dinner;
        private Meal snack;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Macros {
            private String protein_g;
            private String carbs_g;
            private String fat_g;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Meal {
            private String calories;
            @JsonDeserialize(using = PrecautionsDeserializer.class)
            private String menu;
            @JsonDeserialize(using = PrecautionsDeserializer.class)
            private String nutrition;
        }
    }

    /**
     * 自定义反序列化器：兼容 JSON 中的数组或字符串格式
     */
    public static class PrecautionsDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isArray()) {
                List<String> items = new ArrayList<>();
                for (JsonNode item : node) {
                    items.add(item.asText());
                }
                return String.join("\n", items);
            } else if (node.isTextual()) {
                return node.asText();
            }
            return node.toString();
        }
    }
}