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
 * 直接返回AI生成的JSON数组
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationPlanVO {

    private Integer id;
    private String title;
    private Training training_plan;
    private Diet diet_plan;
    private String reason;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Training {
        private String type;
        private String duration;
        private String intensity;
        @JsonDeserialize(using = PrecautionsDeserializer.class)
        private String precautions;
    }

    /**
     * 自定义反序列化器：将数组或字符串转换为字符串
     */
    public static class PrecautionsDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isArray()) {
                // 如果是数组，将所有元素用换行符连接
                List<String> items = new ArrayList<>();
                for (JsonNode item : node) {
                    items.add(item.asText());
                }
                return String.join("\n", items);
            } else if (node.isTextual()) {
                // 如果是字符串，直接返回
                return node.asText();
            } else {
                // 其他情况，返回 JSON 字符串
                return node.toString();
            }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Diet {
        // 兼容旧版字段
        private Meal breakfast;
        private Meal lunch;
        private Meal dinner;
        private Meal snack;

        // 新增字段：总热量
        private String total_calories;

        // 新增字段：宏量营养素
        private Macros macros;

        // 新增字段：食物交换份数
        private FoodExchange food_exchange;

        // 新增字段：营养原则（列表）
        private List<String> nutrition_principles;

        // 新增字段：饮食策略建议
        private String dietary_strategy;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Meal {
            private String calories;
            @JsonDeserialize(using = PrecautionsDeserializer.class)
            private String menu;
            @JsonDeserialize(using = PrecautionsDeserializer.class)
            private String nutrition;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Macros {
            private String protein_g;
            private String protein_percent;
            private String carbs_g;
            private String carbs_percent;
            private String fat_g;
            private String fat_percent;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FoodExchange {
            private String grains;          // 谷薯类份数
            private String vegetables;      // 蔬菜类份数
            private String fruits;          // 水果类份数
            private String protein_foods;   // 畜禽鱼蛋奶类份数
            private String soy_nuts;        // 大豆坚果类份数
            private String oils;            // 油脂类份数
        }
    }
}
