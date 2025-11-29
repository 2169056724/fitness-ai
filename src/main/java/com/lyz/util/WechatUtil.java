package com.lyz.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyz.model.dto.WechatSessionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 微信工具类
 */
@Slf4j
@Component
public class WechatUtil {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 微信登录凭证校验
     * @param code 微信登录凭证
     * @return WechatSessionDTO
     */
    public WechatSessionDTO code2Session(String code) {
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appid, secret, code
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("微信登录响应: {}", response);
            return objectMapper.readValue(response, WechatSessionDTO.class);
        } catch (Exception e) {
            log.error("调用微信登录接口失败", e);
            throw new RuntimeException("微信登录失败: " + e.getMessage());
        }
    }
}

