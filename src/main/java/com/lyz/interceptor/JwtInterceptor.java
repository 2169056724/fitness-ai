package com.lyz.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.common.Result;
import com.lyz.common.UserContext;
import com.lyz.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 拦截除登录外的所有接口，验证Token并将用户信息存储到ThreadLocal
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头获取Token
        String token = getTokenFromRequest(request);
        
        if (token == null || token.isEmpty()) {
            // Token为空，返回401
            writeErrorResponse(response, 401, "未登录或Token无效");
            return false;
        }
        
        // 验证Token
        Claims claims = jwtUtil.getClaimsFromToken(token);
        if (claims == null) {
            writeErrorResponse(response, 401, "Token无效或已过期");
            return false;
        }
        
        // 从Token中获取用户信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String openid = (String) claims.get("openid");
        
        if (userId == null) {
            writeErrorResponse(response, 401, "Token中缺少用户信息");
            return false;
        }
        
        // 将用户信息存储到ThreadLocal
        UserContext.setUserId(userId);
        if (openid != null) {
            UserContext.setOpenid(openid);
        }
        
        log.debug("用户认证成功: userId={}, openid={}", userId, openid);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
    
    /**
     * 从请求头获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀，返回实际的token
            return authHeader.substring(7);
        }
        return authHeader;
    }
    
    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Result<Object> result = Result.error(code, message);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(result);
        
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}

