package com.english.learn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI 调用用 RestTemplate（仅由页面传入的配置发起请求，超时固定）。
 */
@Configuration
public class AiConfig {

    /**
     * AI 返回结构化内容会更长，15s 容易超时导致前端拿到空字符串。
     * 与前端 30s timeout 配合，服务端给到更宽裕的读取时间。
     */
    private static final int AI_REQUEST_TIMEOUT_MS = 60000;

    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AI_REQUEST_TIMEOUT_MS);
        factory.setReadTimeout(AI_REQUEST_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
