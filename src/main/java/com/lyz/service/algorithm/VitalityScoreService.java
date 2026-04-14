package com.lyz.service.algorithm;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 基于 AHP（层次分析法）的每日综合健康评分系统
 */
@Service
public class VitalityScoreService {

    /**
     * 计算每日活力总分 (Score)
     * W1: 体检约束因子 20%
     * W2: 饮食依从性 40%
     * W3: 运动与活跃度 40%
     *
     * @param baseRiskPenalty 基于静态健康的扣分 (0-40)
     * @param dietAdherence 饮食达标率 (基于饮食摄入与目标的偏差)
     * @param activityLevel 运动活跃度评分 (0-100)
     * @return 综合活力分数 (0-100)
     */
    public int calculateVitalityScore(int baseRiskPenalty, BigDecimal dietAdherence, BigDecimal activityLevel) {
        double baseScore = 100.0;
        
        // 饮食依从性处理 (0-100)
        double dietScore = dietAdherence != null ? dietAdherence.doubleValue() : 0.0;
        if (dietScore > 100) dietScore = 100 - (dietScore - 100) * 2; // 惩罚过度摄入
        if (dietScore < 0) dietScore = 0;
        
        // 活动评分处理 (0-100)
        double actScore = activityLevel != null ? activityLevel.doubleValue() : 0.0;
        if (actScore > 100) actScore = 100;
        if (actScore < 0) actScore = 0;
        
        // AHP 权重分配计算
        double w1 = 0.2; // 身体基础状况
        double w2 = 0.4; // 饮食依从
        double w3 = 0.4; // 活动水平
        
        double finalScore = (baseScore - baseRiskPenalty) * w1 
                            + dietScore * w2
                            + actScore * w3;
                            
        if (finalScore > 100) finalScore = 100;
        if (finalScore < 0) finalScore = 0;
                            
        return (int) Math.round(finalScore);
    }
}
