package com.lyz.service;

import com.lyz.model.dto.UpdateUserBasicDTO;
import com.lyz.model.vo.UserBasicInfoVO;
import com.lyz.model.vo.UserLoginVO;

/**
 * 用户服务接口
 */
public interface UserService {
    /**
     * 微信登录
     */
    UserLoginVO wechatLogin(String code);

    /**
     * 更新用户基础信息
     * @param userId
     * @param basicDTO
     * @return
     */
    UserBasicInfoVO updateUserBasic(Long userId, UpdateUserBasicDTO basicDTO);

    /**
     * 根据用户ID查询用户信息
     */
    UserBasicInfoVO getUserBasicInfo(Long userId);

    
}
