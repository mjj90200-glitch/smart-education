package com.smartedu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 *
 * @author SmartEdu Team
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 配置 RestTemplate Bean，设置超时时间
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时时间：10 秒
        factory.setConnectTimeout(10000);
        // 读取超时时间：30 秒
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }
}
