package com.lyz.service;

import com.lyz.model.vo.CalendarHeatmapVO;
import com.lyz.model.vo.CalorieBurnTrendVO;
import com.lyz.model.vo.CompletionRateTrendVO;
import com.lyz.model.vo.NutritionDistributionVO;
import com.lyz.model.vo.WeightBmiTrendVO;

import java.time.LocalDate;

/**
 * 图表数据服务接口
 */
public interface ChartDataService {
    
    /**
     * 获取用户体重&BMI趋势数据
     * @param userId 用户ID
     * @param days 查询天数（默认30天）
     * @return 体重BMI趋势VO
     */
    WeightBmiTrendVO getWeightBmiTrend(Long userId, Integer days);
    
    /**
     * 获取用户训练完成率趋势数据
     * @param userId 用户ID
     * @param days 查询天数（默认30天）
     * @return 完成率趋势VO
     */
    CompletionRateTrendVO getCompletionRateTrend(Long userId, Integer days);
    
    /**
     * 获取用户月打卡热力图数据
     * @param userId 用户ID
     * @param yearMonth 年月，格式：yyyy-MM，如 "2024-01"
     * @return 热力图VO
     */
    CalendarHeatmapVO getCalendarHeatmap(Long userId, String yearMonth);
    
    /**
     * 获取用户营养分配数据（最新一天或指定日期）
     * @param userId 用户ID
     * @param date 日期（为空则取最新）
     * @return 营养分配VO
     */
    NutritionDistributionVO getNutritionDistribution(Long userId, LocalDate date);
    
    /**
     * 获取用户热量消耗趋势数据
     * @param userId 用户ID
     * @param days 查询天数（默认30天）
     * @return 热量消耗趋势VO
     */
    CalorieBurnTrendVO getCalorieBurnTrend(Long userId, Integer days);
}
