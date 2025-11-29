package com.lyz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 体重&BMI趋势图VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeightBmiTrendVO {
    /**
     * 趋势数据点列表
     */
    private List<DataPoint> dataPoints;
    
    /**
     * 当前体重
     */
    private BigDecimal currentWeight;
    
    /**
     * 当前BMI
     */
    private BigDecimal currentBmi;
    
    /**
     * BMI状态：underweight/normal/overweight/obese
     */
    private String bmiStatus;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 体重(kg)
         */
        private BigDecimal weight;
        
        /**
         * BMI值
         */
        private BigDecimal bmi;
    }
}
