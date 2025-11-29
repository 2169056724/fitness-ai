package com.lyz.mapper;

import com.lyz.model.entity.UserRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserRecommendationMapper {
    /**
     * 插入用户已选推荐
     * @param rec
     */
    int insert(UserRecommendation rec);

    /**
     * 查询用户在指定日期范围内的已选推荐（按日期倒序）
     * @param userId 用户ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 推荐列表
     */
    List<UserRecommendation> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询用户最近选择的推荐（用于检测是否首次）
     * @param userId 用户ID
     * @return 最新的推荐记录
     */
    UserRecommendation selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 如果该日期已存在记录则更新，否则插入
     * @param rec
     */
    int insertOrUpdate(UserRecommendation rec);

    /**
     * 根据用户ID和日期查询推荐计划
     * @param userId 用户ID
     * @param date 日期
     * @return 推荐计划
     */
    UserRecommendation getByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
