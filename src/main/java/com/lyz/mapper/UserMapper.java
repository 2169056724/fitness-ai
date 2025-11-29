package com.lyz.mapper;

import com.lyz.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     */
    User selectByOpenid(@Param("openid") String openid);

    /**
     * 根据用户ID查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int updateById(User user);

    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("id") Long id);
}
