package com.lyz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 营养分配图VO - 饼图展示三大营养素占比
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionDistributionVO {
    /**
     * 日期
     */
    private LocalDate date;
    
    /**
     * 总热量
     */
    private BigDecimal totalCalories;
    
    /**
     * 目标热量
     */
    private BigDecimal targetCalories;
    
    /**
     * 营养素分配
     */
    private MacroDistribution macroDistribution;
    
    /**
     * 各餐热量分配
     */
    private MealDistribution mealDistribution;
    
    /**
     * 营养素详情（趋势用）
     */
    private List<DailyMacros> dailyMacrosList;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MacroDistribution {
        /**
         * 蛋白质（g）
         */
        private BigDecimal protein;
        
        /**
         * 碳水化合物（g）
         */
        private BigDecimal carbohydrate;
        
        /**
         * 脂肪（g）
         */
        private BigDecimal fat;
        
        /**
         * 蛋白质占比（%）
         */
        private BigDecimal proteinPercentage;
        
        /**
         * 碳水占比（%）
         */
        private BigDecimal carbPercentage;
        
        /**
         * 脂肪占比（%）
         */
        private BigDecimal fatPercentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealDistribution {
        /**
         * 早餐热量
         */
        private BigDecimal breakfast;
        
        /**
         * 午餐热量
         */
        private BigDecimal lunch;
        
        /**
         * 晚餐热量
         */
        private BigDecimal dinner;
        
        /**
         * 加餐热量
         */
        private BigDecimal snack;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMacros {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 蛋白质（g）
         */
        private BigDecimal protein;
        
        /**
         * 碳水化合物（g）
         */
        private BigDecimal carbohydrate;
        
        /**
         * 脂肪（g）
         */
        private BigDecimal fat;
    }
}
