package com.lyz.mapper;

import com.lyz.model.entity.UserNutritionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户营养记录Mapper
 */
@Mapper
public interface UserNutritionRecordMapper {
    
    /**
     * 插入营养记录（如果日期已存在则更新）
     * @param record 营养记录
     * @return 影响行数
     */
    int insertOrUpdate(UserNutritionRecord record);
    
    /**
     * 根据用户ID和日期范围查询营养记录
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 营养记录列表
     */
    List<UserNutritionRecord> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * 根据用户ID和日期查询单条记录
     * @param userId 用户ID
     * @param recordDate 记录日期
     * @return 营养记录
     */
    UserNutritionRecord selectByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("recordDate") LocalDate recordDate
    );
}
