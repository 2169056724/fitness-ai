package com.lyz.service.impl;

import com.lyz.mapper.ChartDataMapper;
import com.lyz.mapper.UserNutritionRecordMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.entity.UserNutritionRecord;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.vo.CalendarHeatmapVO;
import com.lyz.model.vo.CalorieBurnTrendVO;
import com.lyz.model.vo.CompletionRateTrendVO;
import com.lyz.model.vo.NutritionDistributionVO;
import com.lyz.model.vo.WeightBmiTrendVO;
import com.lyz.service.ChartDataService;
import com.lyz.service.component.NutritionCalculator;
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

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private NutritionCalculator nutritionCalculator;
    
    @Autowired
    private com.lyz.service.algorithm.HealthPredictionService healthPredictionService;
    
    @Autowired
    private com.lyz.service.algorithm.VitalityScoreService vitalityScoreService;
    
    @Autowired
    private com.lyz.mapper.UserFeedbackMapper userFeedbackMapper;

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

        // 使用本地线性回归算法预测下一天体重
        if (!dataPoints.isEmpty()) {
            List<BigDecimal> historicalWeights = new ArrayList<>();
            for (WeightBmiTrendVO.DataPoint dp : dataPoints) {
                historicalWeights.add(dp.getWeight());
            }
            BigDecimal predictedWeight = healthPredictionService.predictNextWeight(historicalWeights);
            vo.setPredictedNextWeight(predictedWeight);
            vo.setPredictedDate(endDate.plusDays(1));
            log.info("用户{}预测下一天体重: {}kg", userId, predictedWeight);
        }

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
            // java.sql.Date 需要转换为 LocalDate
            java.sql.Date sqlDate = (java.sql.Date) feedback.get("date");
            LocalDate date = sqlDate.toLocalDate();
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
            // java.sql.Date 需要转换为 LocalDate
            java.sql.Date sqlDate = (java.sql.Date) checkIn.get("date");
            LocalDate date = sqlDate.toLocalDate();
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

        // 构建三餐热量分配
        NutritionDistributionVO.MealDistribution mealDistribution = new NutritionDistributionVO.MealDistribution();
        mealDistribution
                .setBreakfast(record.getBreakfastCalories() != null ? record.getBreakfastCalories() : BigDecimal.ZERO);
        mealDistribution.setLunch(record.getLunchCalories() != null ? record.getLunchCalories() : BigDecimal.ZERO);
        mealDistribution.setDinner(record.getDinnerCalories() != null ? record.getDinnerCalories() : BigDecimal.ZERO);

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

        // 获取用户档案，计算 BMR（基础代谢）
        UserProfile profile = userProfileMapper.getByUserId(userId);
        BigDecimal dailyBmr = BigDecimal.ZERO;
        if (profile != null) {
            NutritionCalculator.NutritionTarget target = nutritionCalculator.calculate(profile);
            dailyBmr = BigDecimal.valueOf(target.getBmr());
            log.info("用户{}的每日基础代谢(BMR): {} kcal", userId, dailyBmr);
        }

        // 获取该周期内的打卡反馈情况
        List<com.lyz.model.entity.UserFeedback> feedbacks = userFeedbackMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
        java.util.Map<LocalDate, BigDecimal> feedbackMap = new java.util.HashMap<>();
        if (feedbacks != null) {
            for (com.lyz.model.entity.UserFeedback fb : feedbacks) {
                if (fb.getCompletionRate() != null) {
                    feedbackMap.put(fb.getFeedbackDate(), fb.getCompletionRate());
                }
            }
        }

        CalorieBurnTrendVO vo = new CalorieBurnTrendVO();
        List<CalorieBurnTrendVO.DataPoint> dataPoints = new ArrayList<>();

        BigDecimal totalIntake = BigDecimal.ZERO;
        BigDecimal totalBurn = BigDecimal.ZERO;
        int daysOnTarget = 0;

        for (UserNutritionRecord record : records) {
            BigDecimal intake = record.getTotalCalories() != null ? record.getTotalCalories() : BigDecimal.ZERO;
            
            // 理论上的运动总消耗 (AI生成的预估值)
            BigDecimal exerciseBurn = record.getEstimatedBurn() != null ? record.getEstimatedBurn() : BigDecimal.ZERO;
            
            // 结合实际完成率估算真实的运动消耗
            BigDecimal completionRate = feedbackMap.get(record.getRecordDate());
            if (completionRate != null) {
                exerciseBurn = exerciseBurn.multiply(completionRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                // 若没有打卡反馈，说明未完成训练，运动消耗记为 0
                exerciseBurn = BigDecimal.ZERO;
            }

            // 总消耗 = BMR（基础代谢）+ 实际运动消耗
            BigDecimal totalDailyBurn = dailyBmr.add(exerciseBurn);
            BigDecimal net = intake.subtract(totalDailyBurn);

            CalorieBurnTrendVO.DataPoint point = new CalorieBurnTrendVO.DataPoint();
            point.setDate(record.getRecordDate());
            point.setCaloriesIntake(intake);
            point.setCaloriesBurn(totalDailyBurn); // 使用总消耗（BMR + 运动）
            point.setNetCalories(net);
            point.setTargetCalories(record.getTargetCalories());
            point.setExerciseDuration(record.getExerciseDuration());

            dataPoints.add(point);

            totalIntake = totalIntake.add(intake);
            totalBurn = totalBurn.add(totalDailyBurn);

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

    @Override
    public List<Integer> getHealthRiskRadar(Long userId) {
        UserProfile profile = userProfileMapper.getByUserId(userId);
        
        // 默认得分 (0-100, 100为满分健康)
        int bmiScore = 80;
        int bpScore = 90; // 血压
        int bsScore = 90; // 血糖
        int activityScore = 75; // 日常体能
        int sleepScore = 85; // 睡眠作息
        
        if (profile != null) {
            // BMI
            if (profile.getHeightCm() != null && profile.getWeightKg() != null && profile.getHeightCm().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal bmi = profile.getWeightKg().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
                if (bmi.compareTo(new BigDecimal("18.5")) < 0) bmiScore = 70;
                else if (bmi.compareTo(new BigDecimal("24")) < 0) bmiScore = 95;
                else if (bmi.compareTo(new BigDecimal("28")) < 0) bmiScore = 60;
                else bmiScore = 40;
            }
            
            // 简单根据医疗限制扣分 (模拟体检单解析结果联动能力)
            String constraints = (
                (profile.getMedicalHistory() != null ? profile.getMedicalHistory() : "") + " " +
                (profile.getSpecialRestrictions() != null ? profile.getSpecialRestrictions() : "") + " " +
                (profile.getExtractedMedicalData() != null ? profile.getExtractedMedicalData() : "")
            ).toLowerCase();
            if (!constraints.trim().isEmpty()) {
                if (constraints.contains("血压") || constraints.contains("高血压")) bpScore -= 35;
                if (constraints.contains("血糖") || constraints.contains("糖尿病")) bsScore -= 35;
                if (constraints.contains("心") || constraints.contains("关节")) activityScore -= 25;
            }

            // 根据目标调整活动分数
            if ("减脂".equals(profile.getGoal())) {
                activityScore += 5;
            } else if ("增肌".equals(profile.getGoal())) {
                activityScore += 10;
            }
        }
        
        return java.util.Arrays.asList(bmiScore, bpScore, bsScore, activityScore, sleepScore);
    }
    
    @Override
    public Integer getVitalityScore(Long userId) {
        int basePenalty = 0;
        UserProfile profile = userProfileMapper.getByUserId(userId);
        if (profile != null) {
            String constraints = (
                (profile.getMedicalHistory() != null ? profile.getMedicalHistory() : "") + " " +
                (profile.getSpecialRestrictions() != null ? profile.getSpecialRestrictions() : "") + " " +
                (profile.getExtractedMedicalData() != null ? profile.getExtractedMedicalData() : "")
            ).toLowerCase();
            if (constraints.contains("血压")) basePenalty += 10;
            if (constraints.contains("血糖")) basePenalty += 10;
            if (constraints.contains("心")) basePenalty += 15;
            if (constraints.contains("关节")) basePenalty += 5;
        }

        // 饮食依从性计算 (今日)
        LocalDate today = LocalDate.now();
        com.lyz.model.entity.UserNutritionRecord record = userNutritionRecordMapper.selectByUserIdAndDate(userId, today);
        BigDecimal dietAdherence = new BigDecimal("85"); // default
        if (record != null && record.getTargetCalories() != null && record.getTargetCalories().compareTo(BigDecimal.ZERO) > 0) {
             BigDecimal intake = record.getTotalCalories() != null ? record.getTotalCalories() : BigDecimal.ZERO;
             dietAdherence = intake.divide(record.getTargetCalories(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }
        
        // 运动活跃度：基于最近7天反馈的完成率计算真实活跃度
        BigDecimal activityLevel;
        LocalDate weekAgo = today.minusDays(6);
        List<com.lyz.model.entity.UserFeedback> recentFeedbacks =
                userFeedbackMapper.selectByUserIdAndDateRange(userId, weekAgo, today);
        if (recentFeedbacks != null && !recentFeedbacks.isEmpty()) {
            // 用最近反馈的平均完成率作为运动活跃度评分
            BigDecimal totalRate = BigDecimal.ZERO;
            int count = 0;
            for (com.lyz.model.entity.UserFeedback fb : recentFeedbacks) {
                if (fb.getCompletionRate() != null) {
                    totalRate = totalRate.add(fb.getCompletionRate());
                    count++;
                }
            }
            if (count > 0) {
                activityLevel = totalRate.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                log.info("用户{}运动活跃度(基于{}条反馈): {}", userId, count, activityLevel);
            } else {
                activityLevel = new BigDecimal("60"); // 有反馈但无完成率数据时的保守默认值
            }
        } else {
            activityLevel = new BigDecimal("60"); // 无反馈记录时的保守默认值
            log.info("用户{}无近期反馈记录，使用默认运动活跃度: {}", userId, activityLevel);
        }
        
        // 调用自研AHP算法计算最终得分
        return vitalityScoreService.calculateVitalityScore(basePenalty, dietAdherence, activityLevel);
    }
}
