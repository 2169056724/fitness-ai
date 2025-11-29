package com.lyz.config;

import com.lyz.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类，配置拦截器和HTTP客户端
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private JwtInterceptor jwtInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有/api/**路径
                .excludePathPatterns(
                        "/api/user/login/wechat",  // 排除登录接口
                        "/api/ai/test"  // 排除AI测试接口（方便测试）
                );
    }

    /**
     * 配置RestTemplate，用于HTTP请求（如调用微信API）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

