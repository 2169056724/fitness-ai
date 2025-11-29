package com.lyz.model.vo;

import lombok.Data;

/**
 * 用户信息VO
 */
@Data
public class UserBasicInfoVO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatar;
}

