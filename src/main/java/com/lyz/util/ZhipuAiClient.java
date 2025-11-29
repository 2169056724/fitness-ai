package com.lyz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 智谱 AI API 客户端
 */
@Slf4j
@Component
public class ZhipuAiClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final int maxAttempts;
    private final Integer maxTokens;

    public ZhipuAiClient(@Value("${spring.ai.zhipuai.api-key:}") String apiKeyFromConfig,
                         @Value("${spring.ai.zhipuai.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
                         @Value("${spring.ai.zhipuai.timeout.connect-ms:5000}") long connectTimeoutMs,
                         @Value("${spring.ai.zhipuai.timeout.read-ms:20000}") long readTimeoutMs,
                         @Value("${spring.ai.zhipuai.timeout.write-ms:20000}") long writeTimeoutMs,
                         @Value("${spring.ai.zhipuai.timeout.call-ms:60000}") long callTimeoutMs,
                         @Value("${spring.ai.zhipuai.retry.max-attempts:2}") int maxAttempts,
                         @Value("${spring.ai.zhipuai.chat.max-tokens:800}") Integer maxTokens,
                         ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
        String envKey = System.getenv("ZHIPU_KEY");
        String envKey2 = System.getenv("ZHIPU_API_KEY");
        String finalKey = StringUtils.firstNonBlank(apiKeyFromConfig, envKey, envKey2);
        if (StringUtils.isBlank(finalKey)) {
            throw new IllegalStateException("未配置智谱AI的 API Key，请设置 spring.ai.zhipuai.api-key 或环境变量 ZHIPU_KEY / ZHIPU_API_KEY");
        }
        this.apiKey = finalKey.trim();
        this.baseUrl = StringUtils.defaultIfBlank(baseUrl, DEFAULT_BASE_URL);
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.maxTokens = maxTokens != null && maxTokens > 0 ? maxTokens : null;
        log.info("ZhipuAiClient 初始化完成，使用 baseUrl={}", this.baseUrl);
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null, null, null);
    }

    public String chat(String systemPrompt, String userPrompt, String model, Double temperature, Double topP) {
        Map<String, Object> payload = buildPayload(systemPrompt, userPrompt, model, temperature, topP);
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new IllegalStateException("构建智谱AI请求体失败: " + e.getMessage(), e);
        }
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
                Request request = new Request.Builder()
                        .url(baseUrl)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String message = response.body() != null ? response.body().string() : "";
                        log.error("调用智谱GLM失败，code={}, body={}", response.code(), message);
                        throw new IllegalStateException("调用智谱AI失败，HTTP " + response.code());
                    }
                    String responseText = response.body() != null ? response.body().string() : "";
                    return parseContent(responseText);
                }
            } catch (SocketTimeoutException timeout) {
                log.warn("调用智谱AI超时，attempt={}/{}", attempt, maxAttempts);
                if (attempt >= maxAttempts) {
                    throw new IllegalStateException("调用智谱AI异常: timeout", timeout);
                }
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                log.error("调用智谱AI异常", e);
                throw new IllegalStateException("调用智谱AI异常: " + e.getMessage(), e);
            }
        }
        throw new IllegalStateException("调用智谱AI异常: 未知错误");
    }

    private Map<String, Object> buildPayload(String systemPrompt, String userPrompt, String model, Double temperature, Double topP) {
        if (StringUtils.isBlank(userPrompt)) {
            throw new IllegalArgumentException("userPrompt 不能为空");
        }
        List<Map<String, String>> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(systemPrompt)) {
            Map<String, String> system = new HashMap<>();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.add(system);
        }
        Map<String, String> user = new HashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", StringUtils.defaultIfBlank(model, "glm-4.5-air"));
        payload.put("messages", messages);
        payload.put("temperature", temperature != null ? temperature : 0.7);
        payload.put("top_p", topP != null ? topP : 0.7);
        payload.put("stream", Boolean.FALSE);
        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }
        return payload;
    }

    private String parseContent(String responseText) throws IOException {
        JsonNode root = objectMapper.readTree(responseText);
        JsonNode errorNode = root.get("error");
        if (errorNode != null && !errorNode.isNull()) {
            String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : errorNode.toString();
            throw new IllegalStateException("智谱AI返回错误：" + errorMessage);
        }
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("智谱AI未返回有效内容");
        }
        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new IllegalStateException("智谱AI返回内容为空");
        }
        return content.asText();
    }
}


