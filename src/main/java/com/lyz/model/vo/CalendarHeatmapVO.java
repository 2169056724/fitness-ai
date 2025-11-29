package com.lyz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 月打卡热力图VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarHeatmapVO {
    /**
     * 年月，如 "2024-01"
     */
    private String month;
    
    /**
     * 热力图数据点列表
     */
    private List<HeatmapData> heatmapData;
    
    /**
     * 本月打卡天数
     */
    private Integer checkInDays;
    
    /**
     * 本月总天数
     */
    private Integer totalDays;
    
    /**
     * 本月平均完成率
     */
    private BigDecimal averageCompletionRate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapData {
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 是否有反馈记录
         */
        private Boolean hasRecord;
        
        /**
         * 完成率（0-100）
         */
        private BigDecimal completionRate;
        
        /**
         * 强度等级：0-无记录, 1-低(0-60), 2-中(60-80), 3-高(80-100)
         */
        private Integer intensity;
    }
}
