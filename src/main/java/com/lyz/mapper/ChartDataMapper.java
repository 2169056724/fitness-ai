package com.lyz.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 图表数据Mapper
 */
@Mapper
public interface ChartDataMapper {
    
    /**
     * 获取用户的体重和身高（用于计算BMI）
     * @param userId 用户ID
     * @return 包含weightKg和heightCm的Map
     */
    Map<String, BigDecimal> getUserWeightAndHeight(@Param("userId") Long userId);
    
    /**
     * 获取用户指定日期范围内的反馈数据（用于完成率趋势）
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 反馈数据列表
     */
    List<Map<String, Object>> getUserFeedbackTrend(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * 获取用户指定月份的打卡热力图数据
     * @param userId 用户ID
     * @param startDate 月份开始日期
     * @param endDate 月份结束日期
     * @return 打卡数据列表
     */
    List<Map<String, Object>> getUserCheckInHeatmap(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
