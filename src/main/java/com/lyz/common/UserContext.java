package com.lyz.common;

/**
 * 用户上下文，使用ThreadLocal存储当前线程的用户信息
 */
public class UserContext {
    
    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> openidHolder = new ThreadLocal<>();
    
    /**
     * 设置用户ID
     */
    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }
    
    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return userIdHolder.get();
    }
    
    /**
     * 设置openid
     */
    public static void setOpenid(String openid) {
        openidHolder.set(openid);
    }
    
    /**
     * 获取openid
     */
    public static String getOpenid() {
        return openidHolder.get();
    }
    
    /**
     * 清除当前线程的用户信息
     */
    public static void clear() {
        userIdHolder.remove();
        openidHolder.remove();
    }
}

