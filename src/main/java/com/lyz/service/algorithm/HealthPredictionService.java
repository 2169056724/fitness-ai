package com.lyz.service.algorithm;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 基于一元线性回归的健康趋势预测算法
 * 最小二乘法 (Least Squares Method)
 */
@Service
public class HealthPredictionService {
    
    /**
     * 预测未来的体重数据
     * @param historicalWeights 历史连续体重数据
     * @return 预测的下一天的体重
     */
    public BigDecimal predictNextWeight(List<BigDecimal> historicalWeights) {
        if (historicalWeights == null || historicalWeights.size() < 2) {
            return historicalWeights != null && !historicalWeights.isEmpty() 
                   ? historicalWeights.get(historicalWeights.size() - 1) 
                   : BigDecimal.ZERO;
        }

        int n = historicalWeights.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = historicalWeights.get(i).doubleValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return historicalWeights.get(n - 1);
        }

        // 拟合直线: y = ax + b
        double a = (n * sumXY - sumX * sumY) / denominator;
        double b = (sumY - a * sumX) / n;

        // 预测第 n + 1 天的值 (即明天的体重)
        double nextY = a * (n + 1) + b;
        return new BigDecimal(nextY).setScale(1, RoundingMode.HALF_UP);
    }
}
