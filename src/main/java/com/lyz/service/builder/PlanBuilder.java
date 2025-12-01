package com.lyz.service.builder;

import com.lyz.model.vo.RecommendationPlanVO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class PlanBuilder {
    /**
     * 构建深夜休息方案
     */
    public List<RecommendationPlanVO> buildRestPlan() {
        RecommendationPlanVO plan = new RecommendationPlanVO();

        // 1. 设置标题和理由
        plan.setTitle("今日已晚，养精蓄锐");
        plan.setReason("现在时间较晚，高强度运动可能使神经兴奋影响睡眠。建议今晚放松身心，调整状态，明天以饱满的精力开启您的健身之旅！");

        // 2. 构造轻量级训练计划 (放松/冥想)
        RecommendationPlanVO.Training training = new RecommendationPlanVO.Training();
        training.setType("舒缓/助眠");
        training.setDuration("5-10分钟");
        training.setIntensity("极低");
        training.setFocus_part("全身放松");
        training.setMovements(Arrays.asList("腹式呼吸训练 (3分钟)", "睡前冥想 (5分钟)", "肩颈简单拉伸 (2分钟)"));
        training.setPrecautions("请在安静、光线柔和的环境下进行，专注于呼吸，无需追求动作幅度。");
        plan.setTraining_plan(training);

        // 3. 构造饮食建议 (空热量，仅提示)
        RecommendationPlanVO.Diet diet = new RecommendationPlanVO.Diet();
        diet.setTotal_calories(0); // 深夜不建议摄入热量
        diet.setAdvice("睡前2小时建议避免进食，以免增加肠胃负担。如感到饥饿，可饮用少量温水或脱脂热牛奶。");

        // 初始化空对象防止前端空指针
        RecommendationPlanVO.Diet.Macros zeroMacros = new RecommendationPlanVO.Diet.Macros();
        zeroMacros.setProtein_g(0);
        zeroMacros.setCarbs_g(0);
        zeroMacros.setFat_g(0);
        diet.setMacros(zeroMacros);
        diet.setForbidden_categories(Collections.singletonList("夜宵/高糖饮料"));

        // 设置空餐单
        diet.setBreakfast(createEmptyMeal("早餐"));
        diet.setLunch(createEmptyMeal("午餐"));
        diet.setDinner(createEmptyMeal("晚餐"));
        diet.setSnack(createEmptyMeal("加餐"));

        plan.setDiet_plan(diet);

        return Collections.singletonList(plan);
    }

    /**
     * 辅助构建空餐单
     */
    private RecommendationPlanVO.Diet.Meal createEmptyMeal(String name) {
        RecommendationPlanVO.Diet.Meal meal = new RecommendationPlanVO.Diet.Meal();
        meal.setName(name);
        meal.setCalories(0);
        meal.setSuggestion("今日计划已跳过");
        RecommendationPlanVO.Diet.Macros m = new RecommendationPlanVO.Diet.Macros();
        m.setProtein_g(0);
        m.setCarbs_g(0);
        m.setFat_g(0);
        meal.setMacros(m);
        return meal;
    }
}
