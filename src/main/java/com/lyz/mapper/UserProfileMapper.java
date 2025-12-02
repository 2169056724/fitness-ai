package com.lyz.mapper;

import com.lyz.model.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalTime;
import java.util.List;

@Mapper
public interface UserProfileMapper {
    /**
     * 根据用户ID查询健康档案信息
     * @param UserId
     * @return
     */
    UserProfile getByUserId(Long UserId);

    /**
     * 新增用户健康档案
     * @param userProfile
     */
    void addProfile(UserProfile userProfile);

    /**
     * 更新用户健康档案
     * @param userProfile
     */
    void updateProfile(UserProfile userProfile);

    /**
     *
     * @param userProfile
     */
    void upsertProfile(UserProfile userProfile);

    /**
     * 查询所有健康档案（用于定时任务批量生成计划）
     * @return 所有用户的健康档案列表
     */
    List<UserProfile> selectAllProfiles();

    /**
     * 查询活跃用户的健康档案（N天内有登录记录）
     * @param days 天数阈值（如7表示7天内有登录）
     * @return 活跃用户的健康档案列表
     */
    List<UserProfile> selectActiveProfiles(@Param("days") int days);

    /**
     * 查询指定用餐时间段内的用户
     * @param mealType 用餐类型 (breakfast/lunch/dinner/snack)
     * @param startTime 开始时间 (包含)
     * @param endTime 结束时间 (不包含)
     */
    List<UserProfile> selectProfilesByMealTime(
            @Param("mealType") String mealType,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
