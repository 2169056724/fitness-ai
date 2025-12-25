package com.lyz.mapper;

import com.lyz.model.entity.UserWeightLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户体重日志Mapper
 */
@Mapper
public interface UserWeightLogMapper {

    /**
     * 插入体重记录
     */
    int insert(UserWeightLog record);

    /**
     * 插入或更新（同一天更新）
     */
    int insertOrUpdate(UserWeightLog record);

    /**
     * 根据用户ID和日期查询
     */
    UserWeightLog selectByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") LocalDate recordDate);

    /**
     * 查询用户最近N天的体重记录
     */
    List<UserWeightLog> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询用户最新的体重记录
     */
    UserWeightLog selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 删除记录
     */
    int deleteById(@Param("id") Long id);
}
