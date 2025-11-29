package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.vo.CalendarHeatmapVO;
import com.lyz.model.vo.CalorieBurnTrendVO;
import com.lyz.model.vo.CompletionRateTrendVO;
import com.lyz.model.vo.NutritionDistributionVO;
import com.lyz.model.vo.WeightBmiTrendVO;
import com.lyz.service.ChartDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 图表数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chart")
public class ChartDataController {
    
    @Autowired
    private ChartDataService chartDataService;
    
    /**
     * 获取体重&BMI趋势图数据
     */
    @GetMapping("/weight-bmi-trend")
    public Result<WeightBmiTrendVO> getWeightBmiTrend(
           @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        Long userId = UserContext.getUserId();
        WeightBmiTrendVO vo = chartDataService.getWeightBmiTrend(userId, days);
        return Result.success(vo);
    }
    
    /**
     * 获取训练完成率趋势图数据
     */
    @GetMapping("/completion-rate-trend")
    public Result<CompletionRateTrendVO> getCompletionRateTrend(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        Long userId = UserContext.getUserId();
        CompletionRateTrendVO vo = chartDataService.getCompletionRateTrend(userId, days);
        return Result.success(vo);
    }
    
    /**
     * 获取月打卡热力图数据
     */
    @GetMapping("/calendar-heatmap")
    public Result<CalendarHeatmapVO> getCalendarHeatmap(
            @RequestParam(required = false) String yearMonth
    ) {
        Long userId = UserContext.getUserId();
        CalendarHeatmapVO vo = chartDataService.getCalendarHeatmap(userId, yearMonth);
        return Result.success(vo);
    }
    
    /**
     * 获取营养分配数据（饼图）
     */
    @GetMapping("/nutrition-distribution")
    public Result<NutritionDistributionVO> getNutritionDistribution(
            @RequestParam(required = false) 
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        Long userId = UserContext.getUserId();
        NutritionDistributionVO vo = chartDataService.getNutritionDistribution(userId, date);
        return Result.success(vo);
    }
    
    /**
     * 获取热量消耗趋势图数据
     */
    @GetMapping("/calorie-burn-trend")
    public Result<CalorieBurnTrendVO> getCalorieBurnTrend(
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        Long userId = UserContext.getUserId();
        CalorieBurnTrendVO vo = chartDataService.getCalorieBurnTrend(userId, days);
        return Result.success(vo);
    }
}
