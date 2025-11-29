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
        RecommendationPlanVO plan = recommendationService.getTodayPlan(userId);
        return Result.success("获取成功", plan);
    }



}
