package com.lyz.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * AI 推荐生成请求参数
 * 注：用户的定性反馈现从 user_feedback 表自动获取，无需在此 DTO 中传入
 */
@Data
public class RecommendationRequestDTO {

    /**
     * 最近的可穿戴设备指标（可选）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private WearableMetrics wearable;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WearableMetrics {
        /**
         * 日均步数
         */
        private Integer steps;

        /**
         * 平均心率
         */
        private Integer averageHeartRate;

        /**
         * 睡眠时长（小时）
         */
        private Double sleepHours;
    }
}


