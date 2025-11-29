package com.lyz.service;

import com.lyz.model.dto.RecommendationRequestDTO;
import com.lyz.model.vo.RecommendationPlanVO;

import java.util.List;

/**
 * 个性化健身饮食推荐服务
 */
public interface RecommendationService {

    /**
     * 生成当日健身与饮食推荐，返回多套方案（通常为三套：保守/平衡/进阶）
     *
     * @param userId  用户ID
     * @param request 请求参数（活动水平、反馈等，可为空）
     * @return 推荐结果列表
     */
    List<RecommendationPlanVO> generateDailyPlan(Long userId, RecommendationRequestDTO request);

    /**
     * 获取当日已生成或已保存的计划（优先从 Redis 缓存读取）
     * @param userId 用户ID
     * @return 当日单个计划，若不存在则返回 null
     */
    RecommendationPlanVO getTodayPlan(Long userId);

}


