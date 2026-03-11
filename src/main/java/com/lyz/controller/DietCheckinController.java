package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.DietCheckinDTO;
import com.lyz.model.vo.DietCheckinVO;
import com.lyz.service.DietCheckinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 饮食打卡接口
 */
@RestController
@RequestMapping("/api/diet")
@Slf4j
public class DietCheckinController {

    @Autowired
    private DietCheckinService dietCheckinService;

    /**
     * 提交/更新某餐打卡
     * 
     * @param dto 打卡数据 (mealType: 1早餐/2午餐/3晚餐, level: 1-5)
     */
    @PostMapping("/checkin")
    public Result<Void> checkin(@RequestBody DietCheckinDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("饮食打卡, userId={}, mealType={}, level={}", userId, dto.getMealType(), dto.getLevel());

        try {
            dietCheckinService.checkin(userId, dto);
            return Result.success("打卡成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取今日三餐打卡状态
     */
    @GetMapping("/checkin/today")
    public Result<DietCheckinVO> getTodayCheckin() {
        Long userId = UserContext.getUserId();
        DietCheckinVO vo = dietCheckinService.getTodayCheckin(userId);
        return Result.success(vo);
    }
}
