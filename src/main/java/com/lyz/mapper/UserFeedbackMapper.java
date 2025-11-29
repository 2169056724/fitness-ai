package com.lyz.mapper;

import com.lyz.model.entity.UserFeedback;
import com.lyz.model.vo.UserFeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserFeedbackMapper {
    int insert(UserFeedback feedback);

    List<UserFeedback> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    UserFeedback selectLatestByUserId(@Param("userId") Long userId);
}
