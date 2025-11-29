package com.lyz.service.impl;

import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.dto.UpdateUserProfileDTO;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.vo.UserProfileInfoVO;
import com.lyz.service.UserProfileService;
import com.lyz.service.builder.MedicalContextBuilder;
import io.micrometer.common.util.StringUtils;
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
    @Autowired
    private MedicalContextBuilder medicalContextBuilder;

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
        // 1. 先查询旧档案，看看是否有体检数据
        UserProfile oldProfile = userProfileMapper.getByUserId(userId);
        String medicalJson = oldProfile != null ? oldProfile.getExtractedMedicalData() : null;

        // 2. 准备更新实体
        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(profileDTO, userProfile);
        userProfile.setUserId(userId);
        userProfile.setUpdatedAt(LocalDateTime.now());

        // 3. 如果存在体检数据，且本次更新可能影响判定（如性别变更）或直接更新了数据
        if (StringUtils.isNotBlank(medicalJson)) {
            // 优先使用 DTO 里的新性别，没有则用旧档案的
            Integer gender = profileDTO.getGender() != null ? profileDTO.getGender() :
                    (oldProfile != null ? oldProfile.getGender() : 0);

            String newAdvice = medicalContextBuilder.generateMedicalAdvicePrompt(medicalJson, gender);
            userProfile.setMedicalAdvicePrompt(newAdvice);
        }

        userProfileMapper.upsertProfile(userProfile);
        return getUserProfile(userId);
    }

}
