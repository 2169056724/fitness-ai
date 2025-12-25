package com.lyz.service;

import com.lyz.model.dto.WeightLogDTO;
import com.lyz.model.entity.UserWeightLog;

import java.util.List;

/**
 * 体重日志服务接口
 */
public interface WeightLogService {

    /**
     * 记录体重
     * 
     * @param userId 用户ID
     * @param dto    体重数据
     * @return 记录实体
     */
    UserWeightLog logWeight(Long userId, WeightLogDTO dto);

    /**
     * 获取最近N天的体重记录
     * 
     * @param userId 用户ID
     * @param days   天数
     * @return 体重记录列表
     */
    List<UserWeightLog> getRecentWeightLogs(Long userId, int days);

    /**
     * 获取最新的体重记录
     * 
     * @param userId 用户ID
     * @return 最新体重，无记录返回null
     */
    UserWeightLog getLatestWeight(Long userId);
}
