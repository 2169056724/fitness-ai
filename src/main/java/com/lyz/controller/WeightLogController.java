package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.WeightLogDTO;
import com.lyz.model.entity.UserWeightLog;
import com.lyz.service.WeightLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体重日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/weight-log")
public class WeightLogController {

    @Autowired
    private WeightLogService weightLogService;

    /**
     * 记录体重
     * POST /api/weight-log
     */
    @PostMapping
    public Result<UserWeightLog> logWeight(@RequestBody @Validated WeightLogDTO dto) {
        Long userId = UserContext.getUserId();
        UserWeightLog record = weightLogService.logWeight(userId, dto);
        return Result.success(record);
    }

    /**
     * 获取体重历史
     * GET /api/weight-log?days=30
     */
    @GetMapping
    public Result<List<UserWeightLog>> getWeightLogs(
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        Long userId = UserContext.getUserId();
        List<UserWeightLog> logs = weightLogService.getRecentWeightLogs(userId, days);
        return Result.success(logs);
    }

    /**
     * 获取最新体重
     * GET /api/weight-log/latest
     */
    @GetMapping("/latest")
    public Result<UserWeightLog> getLatestWeight() {
        Long userId = UserContext.getUserId();
        UserWeightLog latest = weightLogService.getLatestWeight(userId);
        return Result.success(latest);
    }
}
