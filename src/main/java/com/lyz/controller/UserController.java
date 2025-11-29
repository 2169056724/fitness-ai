package com.lyz.controller;

import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.model.dto.UpdateUserBasicDTO;
import com.lyz.model.vo.UserBasicInfoVO;
import com.lyz.model.vo.UserProfileInfoVO;
import com.lyz.model.vo.UserLoginVO;
import com.lyz.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录
     */
    @PostMapping("/login/wechat")
    public Result<UserLoginVO> wechatLogin(@RequestParam("code") String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code不能为空");
        }
        
        UserLoginVO vo = userService.wechatLogin(code);
        return Result.success("登录成功", vo);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/basicInfo")
    public Result<UserBasicInfoVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        UserBasicInfoVO vo = userService.getUserBasicInfo(userId);
        return Result.success(vo);
    }

    /**
     * 更新用户信息(基础)
     */
    @PutMapping("/basicInfo")
    public Result<UserBasicInfoVO> updateUserBasic(@RequestBody UpdateUserBasicDTO basicDTO) {
        Long userId = UserContext.getUserId();
        UserBasicInfoVO vo = userService.updateUserBasic(userId, basicDTO);
        return Result.success("更新成功", vo);
    }






}
