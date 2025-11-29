package com.lyz.service;

import com.lyz.model.dto.UpdateUserProfileDTO;
import com.lyz.model.vo.UserProfileInfoVO;

public interface UserProfileService {


    /**
     * 新增用户健康档案
     * @param userId
     * @param profileDTO
     * @return
     */
    UserProfileInfoVO addUserProfile(Long userId, UpdateUserProfileDTO profileDTO);


    /**
     * 根据用户ID查询用户健康档案
     * @param userId
     * @return
     */
    UserProfileInfoVO getUserProfile(Long userId);

    /**
     * 更新用户健康档案
     * @param userId
     * @param profileDTO
     * @return
     */
    UserProfileInfoVO updateUserProfile(Long userId, UpdateUserProfileDTO profileDTO);
}
