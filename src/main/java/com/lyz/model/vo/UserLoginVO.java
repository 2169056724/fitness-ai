package com.lyz.model.vo;

import lombok.Data;

/**
 * 用户登录响应VO
 */
@Data
public class UserLoginVO {
    /**
     * 用户ID
     */
    private Long userId;


    /**
     * Token
     */
    private String token;

    /**
     * 是否新用户
     */
    private Boolean isNewUser;

    /**
     * 会话密钥
     */
    private String sessionKey;
}

