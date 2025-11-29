package com.lyz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 训练完成率趋势图VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletionRateTrendVO {
    /**
     * 趋势数据点列表
     */
    private List<DataPoint> dataPoints;
    
    /**
     * 平均完成率
     */
    private BigDecimal averageCompletionRate;
    
    /**
     * 最高完成率
     */
    private BigDecimal maxCompletionRate;
    
    /**
     * 趋势：improving/stable/declining
     */
    private String trend;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 完成率
         */
        private BigDecimal completionRate;
        
        /**
         * 评分
         */
        private Integer rating;
    }
}
