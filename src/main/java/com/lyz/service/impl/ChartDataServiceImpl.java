package com.lyz.service.impl;

import com.lyz.mapper.ChartDataMapper;
import com.lyz.mapper.UserNutritionRecordMapper;
import com.lyz.model.entity.UserNutritionRecord;
import com.lyz.model.vo.CalendarHeatmapVO;
import com.lyz.model.vo.CalorieBurnTrendVO;
import com.lyz.model.vo.CompletionRateTrendVO;
import com.lyz.model.vo.NutritionDistributionVO;
import com.lyz.model.vo.WeightBmiTrendVO;
import com.lyz.service.ChartDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 图表数据服务实现
 */
@Slf4j
@Service
public class ChartDataServiceImpl implements ChartDataService {

    @Autowired
    private ChartDataMapper chartDataMapper;

    @Autowired
    private UserNutritionRecordMapper userNutritionRecordMapper;

    @Autowired
    private com.lyz.mapper.UserWeightLogMapper userWeightLogMapper;

    @Override
    public WeightBmiTrendVO getWeightBmiTrend(Long userId, Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }

        WeightBmiTrendVO vo = new WeightBmiTrendVO();

        // 获取用户身高
        Map<String, BigDecimal> weightHeight = chartDataMapper.getUserWeightAndHeight(userId);

        if (weightHeight == null || weightHeight.get("heightCm") == null) {
            vo.setDataPoints(new ArrayList<>());
            return vo;
        }

        BigDecimal currentHeight = weightHeight.get("heightCm");
        BigDecimal heightM = currentHeight.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // 优先从weight_log表查询真实体重记录
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<com.lyz.model.entity.UserWeightLog> weightLogs = userWeightLogMapper.selectByUserIdAndDateRange(userId,
                startDate, endDate);

        List<WeightBmiTrendVO.DataPoint> dataPoints = new ArrayList<>();
        BigDecimal latestWeight = null;
        BigDecimal latestBmi = null;

        if (weightLogs != null && !weightLogs.isEmpty()) {
            // 使用真实数据
            for (com.lyz.model.entity.UserWeightLog log : weightLogs) {
                BigDecimal weight = log.getWeightKg();
                BigDecimal bmi = weight.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
                dataPoints.add(new WeightBmiTrendVO.DataPoint(log.getRecordDate(), weight, bmi));
                latestWeight = weight;
                latestBmi = bmi;
            }
            log.info("用户{}体重趋势：使用{}条真实记录", userId, weightLogs.size());
        } else {
            // 无记录时，使用profile中的初始体重生成fallback数据
            BigDecimal profileWeight = weightHeight.get("weightKg");
            if (profileWeight != null) {
                latestWeight = profileWeight;
                latestBmi = profileWeight.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);
                dataPoints = generateWeightTrendData(profileWeight, latestBmi, currentHeight, days);
                log.info("用户{}体重趋势：使用模拟数据（无记录）", userId);
            }
        }

        vo.setCurrentWeight(latestWeight);
        vo.setCurrentBmi(latestBmi);
        vo.setBmiStatus(latestBmi != null ? getBmiStatus(latestBmi) : null);
        vo.setDataPoints(dataPoints);

        return vo;
    }

    @Override
    public CompletionRateTrendVO getCompletionRateTrend(Long userId, Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 查询反馈数据
        List<Map<String, Object>> feedbackList = chartDataMapper.getUserFeedbackTrend(userId, startDate, endDate);

        CompletionRateTrendVO vo = new CompletionRateTrendVO();
        List<CompletionRateTrendVO.DataPoint> dataPoints = new ArrayList<>();

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;

        for (Map<String, Object> feedback : feedbackList) {
            LocalDate date = (LocalDate) feedback.get("date");
            BigDecimal completionRate = (BigDecimal) feedback.get("completionRate");
            Integer rating = (Integer) feedback.get("rating");

            dataPoints.add(new CompletionRateTrendVO.DataPoint(date, completionRate, rating));

            sum = sum.add(completionRate);
            if (completionRate.compareTo(max) > 0) {
                max = completionRate;
            }
        }

        vo.setDataPoints(dataPoints);
        vo.setMaxCompletionRate(max);

        if (!dataPoints.isEmpty()) {
            BigDecimal average = sum.divide(new BigDecimal(dataPoints.size()), 2, RoundingMode.HALF_UP);
            vo.setAverageCompletionRate(average);
            vo.setTrend(calculateTrend(dataPoints));
        } else {
            vo.setAverageCompletionRate(BigDecimal.ZERO);
            vo.setTrend("stable");
        }

        return vo;
    }

    @Override
    public CalendarHeatmapVO getCalendarHeatmap(Long userId, String yearMonth) {
        // 解析年月
        YearMonth ym;
        if (yearMonth == null || yearMonth.isEmpty()) {
            ym = YearMonth.now();
        } else {
            ym = YearMonth.parse(yearMonth);
        }

        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 查询该月的打卡数据
        List<Map<String, Object>> checkInList = chartDataMapper.getUserCheckInHeatmap(userId, startDate, endDate);

        CalendarHeatmapVO vo = new CalendarHeatmapVO();
        vo.setMonth(ym.toString());
        vo.setTotalDays(ym.lengthOfMonth());

        // 构建热力图数据
        List<CalendarHeatmapVO.HeatmapData> heatmapData = new ArrayList<>();
        Map<LocalDate, BigDecimal> dateCompletionMap = new java.util.HashMap<>();

        // 将查询结果放入Map便于查找
        for (Map<String, Object> checkIn : checkInList) {
            LocalDate date = (LocalDate) checkIn.get("date");
            BigDecimal completionRate = (BigDecimal) checkIn.get("completionRate");
            dateCompletionMap.put(date, completionRate);
        }

        // 遍历该月的每一天
        BigDecimal totalCompletionRate = BigDecimal.ZERO;
        int checkInDays = 0;

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            BigDecimal completionRate = dateCompletionMap.get(date);

            boolean hasRecord = completionRate != null;
            if (hasRecord) {
                checkInDays++;
                totalCompletionRate = totalCompletionRate.add(completionRate);
            } else {
                completionRate = BigDecimal.ZERO;
            }

            Integer intensity = calculateIntensity(hasRecord, completionRate);
            heatmapData.add(new CalendarHeatmapVO.HeatmapData(date, hasRecord, completionRate, intensity));
        }

        vo.setHeatmapData(heatmapData);
        vo.setCheckInDays(checkInDays);

        if (checkInDays > 0) {
            BigDecimal average = totalCompletionRate.divide(new BigDecimal(checkInDays), 2, RoundingMode.HALF_UP);
            vo.setAverageCompletionRate(average);
        } else {
            vo.setAverageCompletionRate(BigDecimal.ZERO);
        }

        return vo;
    }

    /**
     * 判断BMI状态
     */
    private String getBmiStatus(BigDecimal bmi) {
        if (bmi.compareTo(new BigDecimal("18.5")) < 0) {
            return "underweight";
        } else if (bmi.compareTo(new BigDecimal("24")) < 0) {
            return "normal";
        } else if (bmi.compareTo(new BigDecimal("28")) < 0) {
            return "overweight";
        } else {
            return "obese";
        }
    }

    /**
     * 生成模拟的体重趋势数据
     * 实际应用中应该从weight_history表查询
     */
    private List<WeightBmiTrendVO.DataPoint> generateWeightTrendData(BigDecimal currentWeight, BigDecimal currentBmi,
            BigDecimal heightCm, Integer days) {
        List<WeightBmiTrendVO.DataPoint> dataPoints = new ArrayList<>();

        // 模拟数据：假设体重在过去days天内有轻微波动
        LocalDate today = LocalDate.now();

        // 计算身高（米）
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // 生成过去days天的模拟数据（每隔几天一个点）
        int interval = Math.max(1, days / 10); // 最多生成10个数据点

        for (int i = days - 1; i >= 0; i -= interval) {
            LocalDate date = today.minusDays(i);

            // 模拟体重变化（±2kg范围内随机波动）
            double variation = Math.sin(i * 0.2) * 1.5; // 使用正弦函数模拟自然波动
            BigDecimal weight = currentWeight.add(new BigDecimal(variation)).setScale(1, RoundingMode.HALF_UP);

            // 计算对应的BMI
            BigDecimal bmi = weight.divide(heightM.multiply(heightM), 2, RoundingMode.HALF_UP);

            dataPoints.add(new WeightBmiTrendVO.DataPoint(date, weight, bmi));
        }

        // 添加今天的数据
        dataPoints.add(new WeightBmiTrendVO.DataPoint(today, currentWeight, currentBmi));

        return dataPoints;
    }

    /**
     * 计算趋势：improving/stable/declining
     */
    private String calculateTrend(List<CompletionRateTrendVO.DataPoint> dataPoints) {
        if (dataPoints.size() < 2) {
            return "stable";
        }

        // 比较前半段和后半段的平均值
        int mid = dataPoints.size() / 2;
        BigDecimal firstHalfSum = BigDecimal.ZERO;
        BigDecimal secondHalfSum = BigDecimal.ZERO;

        for (int i = 0; i < mid; i++) {
            firstHalfSum = firstHalfSum.add(dataPoints.get(i).getCompletionRate());
        }

        for (int i = mid; i < dataPoints.size(); i++) {
            secondHalfSum = secondHalfSum.add(dataPoints.get(i).getCompletionRate());
        }

        BigDecimal firstAvg = firstHalfSum.divide(new BigDecimal(mid), 2, RoundingMode.HALF_UP);
        BigDecimal secondAvg = secondHalfSum.divide(new BigDecimal(dataPoints.size() - mid), 2, RoundingMode.HALF_UP);

        BigDecimal diff = secondAvg.subtract(firstAvg);

        // 如果差值大于5，认为是improving；小于-5，认为是declining
        if (diff.compareTo(new BigDecimal("5")) > 0) {
            return "improving";
        } else if (diff.compareTo(new BigDecimal("-5")) < 0) {
            return "declining";
        } else {
            return "stable";
        }
    }

    /**
     * 计算强度等级
     * 0-无记录, 1-低(0-60), 2-中(60-80), 3-高(80-100)
     */
    private Integer calculateIntensity(boolean hasRecord, BigDecimal completionRate) {
        if (!hasRecord) {
            return 0;
        }

        if (completionRate.compareTo(new BigDecimal("60")) < 0) {
            return 1;
        } else if (completionRate.compareTo(new BigDecimal("80")) < 0) {
            return 2;
        } else {
            return 3;
        }
    }

    @Override
    public NutritionDistributionVO getNutritionDistribution(Long userId, LocalDate date) {
        // 如果日期为空，使用今天
        if (date == null) {
            date = LocalDate.now();
        }

        // 查询指定日期的营养记录
        UserNutritionRecord record = userNutritionRecordMapper.selectByUserIdAndDate(userId, date);

        if (record == null) {
            // 如果没有记录，返回空VO
            return new NutritionDistributionVO();
        }

        NutritionDistributionVO vo = new NutritionDistributionVO();
        vo.setDate(record.getRecordDate());
        vo.setTotalCalories(record.getTotalCalories());
        vo.setTargetCalories(record.getTargetCalories());

        // 构建营养素分配
        BigDecimal protein = record.getProtein() != null ? record.getProtein() : BigDecimal.ZERO;
        BigDecimal carb = record.getCarbohydrate() != null ? record.getCarbohydrate() : BigDecimal.ZERO;
        BigDecimal fat = record.getFat() != null ? record.getFat() : BigDecimal.ZERO;

        // 计算总克数
        BigDecimal totalGrams = protein.add(carb).add(fat);

        NutritionDistributionVO.MacroDistribution macroDistribution = new NutritionDistributionVO.MacroDistribution();
        macroDistribution.setProtein(protein);
        macroDistribution.setCarbohydrate(carb);
        macroDistribution.setFat(fat);

        // 计算百分比
        if (totalGrams.compareTo(BigDecimal.ZERO) > 0) {
            macroDistribution.setProteinPercentage(
                    protein.divide(totalGrams, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1,
                            RoundingMode.HALF_UP));
            macroDistribution.setCarbPercentage(
                    carb.divide(totalGrams, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1,
                            RoundingMode.HALF_UP));
            macroDistribution.setFatPercentage(
                    fat.divide(totalGrams, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(1,
                            RoundingMode.HALF_UP));
        }

        vo.setMacroDistribution(macroDistribution);

        // 构建各餐热量分配
        NutritionDistributionVO.MealDistribution mealDistribution = new NutritionDistributionVO.MealDistribution();
        mealDistribution
                .setBreakfast(record.getBreakfastCalories() != null ? record.getBreakfastCalories() : BigDecimal.ZERO);
        mealDistribution.setLunch(record.getLunchCalories() != null ? record.getLunchCalories() : BigDecimal.ZERO);
        mealDistribution.setDinner(record.getDinnerCalories() != null ? record.getDinnerCalories() : BigDecimal.ZERO);
        mealDistribution.setSnack(record.getSnackCalories() != null ? record.getSnackCalories() : BigDecimal.ZERO);

        vo.setMealDistribution(mealDistribution);

        // 可选：查询最近7天的营养趋势数据
        LocalDate startDate = date.minusDays(6);
        List<UserNutritionRecord> recentRecords = userNutritionRecordMapper.selectByUserIdAndDateRange(userId,
                startDate, date);
        List<NutritionDistributionVO.DailyMacros> dailyMacrosList = new ArrayList<>();

        for (UserNutritionRecord r : recentRecords) {
            dailyMacrosList.add(new NutritionDistributionVO.DailyMacros(
                    r.getRecordDate(),
                    r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO,
                    r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO,
                    r.getFat() != null ? r.getFat() : BigDecimal.ZERO));
        }

        vo.setDailyMacrosList(dailyMacrosList);

        return vo;
    }

    @Override
    public CalorieBurnTrendVO getCalorieBurnTrend(Long userId, Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        // 查询营养记录
        List<UserNutritionRecord> records = userNutritionRecordMapper.selectByUserIdAndDateRange(userId, startDate,
                endDate);

        CalorieBurnTrendVO vo = new CalorieBurnTrendVO();
        List<CalorieBurnTrendVO.DataPoint> dataPoints = new ArrayList<>();

        BigDecimal totalIntake = BigDecimal.ZERO;
        BigDecimal totalBurn = BigDecimal.ZERO;
        int daysOnTarget = 0;

        for (UserNutritionRecord record : records) {
            BigDecimal intake = record.getTotalCalories() != null ? record.getTotalCalories() : BigDecimal.ZERO;
            BigDecimal burn = record.getEstimatedBurn() != null ? record.getEstimatedBurn() : BigDecimal.ZERO;
            BigDecimal net = intake.subtract(burn);

            CalorieBurnTrendVO.DataPoint point = new CalorieBurnTrendVO.DataPoint();
            point.setDate(record.getRecordDate());
            point.setCaloriesIntake(intake);
            point.setCaloriesBurn(burn);
            point.setNetCalories(net);
            point.setTargetCalories(record.getTargetCalories());
            point.setExerciseDuration(record.getExerciseDuration());

            dataPoints.add(point);

            totalIntake = totalIntake.add(intake);
            totalBurn = totalBurn.add(burn);

            // 判断是否达标（摄入在目标±200范围内）
            if (record.getTargetCalories() != null) {
                BigDecimal diff = intake.subtract(record.getTargetCalories()).abs();
                if (diff.compareTo(new BigDecimal("200")) <= 0) {
                    daysOnTarget++;
                }
            }
        }

        vo.setDataPoints(dataPoints);

        int recordCount = records.size();
        if (recordCount > 0) {
            vo.setAverageIntake(totalIntake.divide(new BigDecimal(recordCount), 2, RoundingMode.HALF_UP));
            vo.setAverageBurn(totalBurn.divide(new BigDecimal(recordCount), 2, RoundingMode.HALF_UP));
            vo.setAverageNet(vo.getAverageIntake().subtract(vo.getAverageBurn()));
        } else {
            vo.setAverageIntake(BigDecimal.ZERO);
            vo.setAverageBurn(BigDecimal.ZERO);
            vo.setAverageNet(BigDecimal.ZERO);
        }

        vo.setDaysOnTarget(daysOnTarget);
        vo.setTotalDays(recordCount);

        return vo;
    }
}
