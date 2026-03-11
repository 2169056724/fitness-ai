package com.lyz.service;

import com.lyz.model.dto.DietCheckinDTO;
import com.lyz.model.vo.DietCheckinVO;

/**
 * 饮食打卡服务
 */
public interface DietCheckinService {

    /**
     * 提交或更新某餐打卡
     */
    void checkin(Long userId, DietCheckinDTO dto);

    /**
     * 获取今日三餐打卡状态
     */
    DietCheckinVO getTodayCheckin(Long userId);
}
