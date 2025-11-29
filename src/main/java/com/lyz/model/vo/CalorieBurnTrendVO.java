package com.lyz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 热量消耗趋势图VO - 展示摄入vs消耗vs净值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalorieBurnTrendVO {
    /**
     * 趋势数据点列表
     */
    private List<DataPoint> dataPoints;
    
    /**
     * 平均每日摄入热量
     */
    private BigDecimal averageIntake;
    
    /**
     * 平均每日消耗热量
     */
    private BigDecimal averageBurn;
    
    /**
     * 平均净摄入（摄入-消耗）
     */
    private BigDecimal averageNet;
    
    /**
     * 达标天数（摄入在目标范围内的天数）
     */
    private Integer daysOnTarget;
    
    /**
     * 总天数
     */
    private Integer totalDays;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 摄入热量
         */
        private BigDecimal caloriesIntake;
        
        /**
         * 运动消耗热量
         */
        private BigDecimal caloriesBurn;
        
        /**
         * 净摄入（摄入-消耗）
         */
        private BigDecimal netCalories;
        
        /**
         * 目标热量
         */
        private BigDecimal targetCalories;
        
        /**
         * 运动时长（分钟）
         */
        private Integer exerciseDuration;
    }
}
