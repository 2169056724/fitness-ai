package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.UpdateUserProfileDTO;
import com.lyz.model.vo.UserProfileInfoVO;
import com.lyz.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/userProfile")
@Slf4j
public class UserProfileController {
    @Autowired
    private UserProfileService userProfileService;

    /**
     * 新增用户健康档案
     *
     * @param profileDTO 用户健康信息
     * @return 用户健康档案视图对象
     */
    @PostMapping()
    public Result<UserProfileInfoVO> AddUserProfile(@RequestBody UpdateUserProfileDTO profileDTO) {
        Long userId = UserContext.getUserId();
        UserProfileInfoVO vo = userProfileService.addUserProfile(userId, profileDTO);
        return Result.success("创建成功", vo);
    }

    /**
     * 获取用户健康档案
     *
     * @return 用户健康档案视图对象
     */
    @GetMapping()
    public Result<UserProfileInfoVO> getUserProfile() {
        Long userId = UserContext.getUserId();
        UserProfileInfoVO vo = userProfileService.getUserProfile(userId);
        return Result.success(vo);
    }

    /**
     * 更新用户健康档案
     * @param profileDTO
     * @return
     */
    @PutMapping
    public Result<UserProfileInfoVO> updateProfile(@RequestBody UpdateUserProfileDTO profileDTO) {
        Long userId = UserContext.getUserId();
        UserProfileInfoVO vo = userProfileService.updateUserProfile(userId, profileDTO);
        return Result.success("更新成功", vo);
    }
}
