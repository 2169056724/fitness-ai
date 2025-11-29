package com.lyz.service.impl;

import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.dto.UpdateUserProfileDTO;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.vo.UserProfileInfoVO;
import com.lyz.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    /**
     * 新增健康档案
     * @param userId
     * @param profileDTO
     * @return
     */
    @Override
    public UserProfileInfoVO addUserProfile(Long userId, UpdateUserProfileDTO profileDTO) {
        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(profileDTO, userProfile);
        userProfile.setUserId(userId);
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.upsertProfile(userProfile);
        return getUserProfile(userId);
    }

    /**
     * 获取健康档案
     * @param userId
     * @return
     */
    @Override
    public UserProfileInfoVO getUserProfile(Long userId) {
        UserProfile userProfile = userProfileMapper.getByUserId(userId);
        if (userProfile == null) {
            throw new RuntimeException("用户健康档案不存在");
        }
        UserProfileInfoVO vo = new UserProfileInfoVO();
        BeanUtils.copyProperties(userProfile, vo);
        return vo;
    }

    /**
     * 更新健康档案
     * @param userId
     * @param profileDTO
     * @return
     */
    @Override
    public UserProfileInfoVO updateUserProfile(Long userId, UpdateUserProfileDTO profileDTO) {
        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(profileDTO, userProfile);
        userProfile.setUserId(userId);
        userProfile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.upsertProfile(userProfile);
        return getUserProfile(userId);
    }

}
