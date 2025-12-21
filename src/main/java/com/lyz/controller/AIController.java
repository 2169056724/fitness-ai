package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.RecommendationRequestDTO;
import com.lyz.model.vo.RecommendationPlanVO;
import com.lyz.service.RecommendationService;
import com.lyz.util.ZhipuAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 相关接口
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
@RequiredArgsConstructor
public class AIController {

    private final ZhipuAiClient zhipuAiClient;
    private final RecommendationService recommendationService;

    /**
     * 智谱接口自测
     */
    @GetMapping("/test")
    public String chat(@RequestParam(name = "query") String query) {
        log.info("测试调用智谱GLM-4.5，消息: {}", query);
        return zhipuAiClient.chat("你是一个友好的助手，用中文简洁回复。", query);
    }

    /**
     * 生成今日健身与饮食推荐
     */
    @PostMapping("/plan")
    public Result<List<RecommendationPlanVO>> generatePlan(@RequestBody(required = false) RecommendationRequestDTO request) {
        Long userId = UserContext.getUserId();
        java.util.List<RecommendationPlanVO> plans = recommendationService.generateDailyPlan(userId, request);
        return Result.success("生成成功", plans);
    }

    /**
     * 获取当日已生成或已保存的计划
     */
    @GetMapping("/plan/today")
    public Result<RecommendationPlanVO> getTodayPlan() {
        Long userId = UserContext.getUserId();
        // 1. 先查缓存/数据库
        log.info("用户 {} 获取今日计划", userId);
        RecommendationPlanVO plan = recommendationService.getTodayPlan(userId);

        // 2. 【核心修改】如果没查到（说明是回归用户，定时任务没给他跑），现场生成
        if (plan == null) {
            log.info("用户 {} 暂无今日计划（可能是回归用户），触发实时生成", userId);
            // 这一步会根据断练时长，自动决定是 AI 生成还是 Java 返回复健操
            List<RecommendationPlanVO> newPlans = recommendationService.generateDailyPlan(userId, null);
            if (!newPlans.isEmpty()) {
                plan = newPlans.get(0);
            }
        }
        return Result.success("获取成功", plan);
    }



}
