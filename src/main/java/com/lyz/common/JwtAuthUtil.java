package com.lyz.common;

import com.lyz.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JWT认证工具类
 */
@Slf4j
@Component
public class JwtAuthUtil {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从请求头中获取Token
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 从请求中获取用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token == null) {
            return false;
        }
        return jwtUtil.validateToken(token);
    }
}

