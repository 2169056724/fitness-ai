package com.lyz.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        private String type; // 训练类型
        private String duration; // 时长
        private String intensity; // 强度
        private String focus_part; // 重点部位

        // 动作列表 (兼容旧版纯字符串数组和新版对象数组)
        @JsonDeserialize(using = MovementsDeserializer.class)
        private List<MovementVO> movements;

        // 注意事项 (兼容字符串或数组)
        @JsonDeserialize(using = PrecautionsDeserializer.class)
        private String precautions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Diet {
        // === 核心新字段 (对应 Prompt) ===
        private Integer total_calories; // 总热量
        private Macros macros; // 宏量营养素
        private List<String> forbidden_categories; // 今日禁忌
        private String advice; // 饮食建议/策略

        // === AI生成的每餐食物参考 ===
        private String breakfast_guide; // 早餐食物参考 (生活化单位)
        private String lunch_guide; // 午餐食物参考
        private String dinner_guide; // 晚餐食物参考

        private Meal breakfast;
        private Meal lunch;
        private Meal dinner;
        private Meal snack;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Macros {
            private Integer protein_g;
            private Integer carbs_g;
            private Integer fat_g;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Meal {
            private String name; // "早餐"
            private Integer calories; // 本餐热量
            private Macros macros; // 本餐营养素
            private String suggestion; // 简单的建议 (后端生成或固定模板)
            private String foodGuide; // 食物参考 (小白友好的量化建议)
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

    /**
     * 动作列表反序列化器：兼容两种格式
     * 1. 旧格式 (AI输出): ["深蹲 (3x12)", "卧推 (4x10)"]
     * 2. 新格式 (已增强): [{name:"深蹲", detail:"3x12", gifUrl:...}, ...]
     */
    public static class MovementsDeserializer extends JsonDeserializer<List<MovementVO>> {
        @Override
        public List<MovementVO> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<MovementVO> result = new ArrayList<>();

            if (!node.isArray()) {
                return result;
            }

            ObjectMapper mapper = (ObjectMapper) p.getCodec();

            for (JsonNode item : node) {
                if (item.isTextual()) {
                    // 旧格式: 纯字符串 "深蹲 (3x12)"
                    result.add(parseStringMovement(item.asText()));
                } else if (item.isObject()) {
                    // 新格式: JSON 对象
                    MovementVO vo = mapper.treeToValue(item, MovementVO.class);
                    result.add(vo);
                }
            }

            return result;
        }

        /**
         * 将字符串格式的动作解析为 MovementVO
         * 示例: "杠铃深蹲 (4x8)" -> name="杠铃深蹲", detail="(4x8)"
         * 示例: "平板支撑 60秒" -> name="平板支撑", detail="60秒"
         */
        private MovementVO parseStringMovement(String raw) {
            MovementVO vo = new MovementVO();
            if (raw == null || raw.isBlank()) {
                vo.setName("未知动作");
                return vo;
            }

            // 尝试匹配括号中的组次信息: "动作名 (3x12)" 或 "动作名 （3x12）"
            String trimmed = raw.trim();
            int parenIdx = findDetailStart(trimmed);

            if (parenIdx > 0) {
                vo.setName(trimmed.substring(0, parenIdx).trim());
                vo.setDetail(trimmed.substring(parenIdx).trim());
            } else {
                // 没有明确分隔符，尝试从末尾提取数字部分
                String[] parts = trimmed.split("\\s+");
                if (parts.length > 1) {
                    String last = parts[parts.length - 1];
                    if (last.matches(".*\\d+.*")) {
                        vo.setName(trimmed.substring(0, trimmed.lastIndexOf(last)).trim());
                        vo.setDetail(last);
                    } else {
                        vo.setName(trimmed);
                    }
                } else {
                    vo.setName(trimmed);
                }
            }

            return vo;
        }

        /** 查找详情部分的起始位置 (括号或组次标记) */
        private int findDetailStart(String s) {
            // 匹配中英文括号
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '(' || c == '（') {
                    return i;
                }
            }
            return -1;
        }
    }
}