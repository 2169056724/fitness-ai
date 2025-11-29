package com.lyz.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
public class User {
    /**
     * 主键
     */
    private Long id;

    /**
     * 微信 openid
     */
    private String openid;


    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;


    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除 0未删除 1已删除
     */
    private Integer isDeleted;
}

