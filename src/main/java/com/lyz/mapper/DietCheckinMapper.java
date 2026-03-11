package com.lyz.mapper;

import com.lyz.model.entity.DietCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食打卡Mapper
 */
@Mapper
public interface DietCheckinMapper {

    /**
     * 插入或更新打卡记录
     */
    int insertOrUpdate(DietCheckin record);

    /**
     * 查询用户某日所有打卡记录
     */
    List<DietCheckin> selectByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("checkinDate") LocalDate checkinDate);
}
