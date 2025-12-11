package com.lyz.service.component;

import com.lyz.model.entity.UserProfile;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 营养学计算核心组件
 * 基于 Mifflin-St Jeor 公式与教科书级营养素分配逻辑
 */
@Component
public class NutritionCalculator {

    @Data
    @Builder
    public static class NutritionTarget {
        private int dailyCalories;    // 每日目标热量 (kcal)
        private int proteinGrams;     // 蛋白质 (g)
        private int fatGrams;         // 脂肪 (g)
        private int carbGrams;        // 碳水 (g)
        private double bmr;           // 基础代谢
        private double tdee;          // 总能量消耗
    }

    /**
     * 计算核心方法
     */
    public NutritionTarget calculate(UserProfile profile) {
        // 1. 基础数据校验与默认值
        double weight = profile.getWeightKg() != null ? profile.getWeightKg().doubleValue() : 60.0;
        double height = profile.getHeightCm() != null ? profile.getHeightCm().doubleValue() : 170.0;
        int age = profile.getAge() != null ? profile.getAge() : 25;
        int gender = profile.getGender() != null ? profile.getGender() : 1; // 1男 2女

        // 2. 计算 BMR (Mifflin-St Jeor 公式)
        // Men: 10W + 6.25H - 5A + 5
        // Women: 10W + 6.25H - 5A - 161
        double bmr = (10 * weight) + (6.25 * height) - (5 * age);
        bmr += (gender == 1 ? 5 : -161);

        // 3. 计算 TDEE (Total Daily Energy Expenditure)
        double activityMultiplier = getActivityMultiplier(profile.getActivityLevel());
        double tdee = bmr * activityMultiplier;

        // 4. 根据目标调整热量缺口/盈余
        double targetCalories = adjustCaloriesByGoal(tdee, profile.getGoal());

        // 5. 计算宏观营养素 (Macros) - 这一步是比之前版本高级的关键
        // 蛋白质热量: 4kcal/g, 碳水: 4kcal/g, 脂肪: 9kcal/g
        return calculateMacros(targetCalories, weight, profile.getGoal(), bmr, tdee);
    }

    private double getActivityMultiplier(String level) {
        // 对应 UserProfile 中的 activityLevel 注释
        return switch (StringUtils.defaultString(level, "中等")) {
            case "久坐" -> 1.2;
            case "轻度" -> 1.375;   // 每周1-3次运动
            case "中等" -> 1.55;    // 每周3-5次
            case "重度" -> 1.725;   // 每周6-7次
            case "运动员" -> 1.9;   // 体力工作或双练
            default -> 1.55;
        };
    }

    private double adjustCaloriesByGoal(double tdee, String goal) {
        if (StringUtils.isBlank(goal)) return tdee;

        return switch (goal) {
            // 减脂：建议制造 15%-20% 的热量缺口，比固定减500更安全
            case "减脂" -> tdee * 0.85;
            // 增肌：建议 10%-15% 的热量盈余
            case "增肌" -> tdee * 1.10;
            // 塑形/维护：保持热量平衡，靠高蛋白改变身体成分
            case "塑形", "健康维护" -> tdee;
            default -> tdee;
        };
    }

    private NutritionTarget calculateMacros(double targetCal, double weightKg, String goal, double bmr, double tdee) {
        // === 蛋白质系数设定 (g/kg体重) ===
        // 减脂/塑形期需要更高蛋白来防止掉肌肉 (2.0-2.2g)
        // 增肌期 (1.8-2.0g)
        // 维持期 (1.5g)
        double proteinRatio = switch (StringUtils.defaultString(goal)) {
            case "减脂", "塑形" -> 2.0;
            case "增肌" -> 1.8;
            default -> 1.5;
        };

        // === 脂肪系数设定 (能量占比) ===
        // 通常建议脂肪占总热量的 25%-30%
        double fatCaloriePercent = 0.25;

        // 1. 计算蛋白质
        int proteinG = (int) (weightKg * proteinRatio);

        // 2. 计算脂肪 (保证最低摄入量，防止激素紊乱)
        int fatG = (int) ((targetCal * fatCaloriePercent) / 9.0);
        if (fatG < (weightKg * 0.8)) { // 兜底：脂肪不低于 0.8g/kg
            fatG = (int) (weightKg * 0.8);
        }

        // 3. 计算碳水 (剩余热量全部给碳水)
        // 碳水 = (总热量 - 蛋白热量 - 脂肪热量) / 4
        double occupiedCal = (proteinG * 4) + (fatG * 9);
        double remainCal = targetCal - occupiedCal;
        int carbG = (int) (remainCal / 4);

        // 4. 极端情况兜底 (如果算出负数，说明热量设太低了)
        if (carbG < 50) carbG = 50; // 脑力活动最低需求

        // 重新反推总热量 (保持对齐)
        int finalCalories = (proteinG * 4) + (fatG * 9) + (carbG * 4);

        return NutritionTarget.builder()
                .dailyCalories(finalCalories)
                .proteinGrams(proteinG)
                .fatGrams(fatG)
                .carbGrams(carbG)
                .bmr(round(bmr))
                .tdee(round(tdee))
                .build();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
