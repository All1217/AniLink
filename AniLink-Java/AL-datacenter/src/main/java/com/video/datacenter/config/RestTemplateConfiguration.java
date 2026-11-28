package com.video.datacenter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** RestTemplate 配置：调用 Flask 引擎时必须有短超时，避免引擎故障拖垮数据中心接口。 */
@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestTemplate restTemplate(EngineProperties engineProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(engineProperties.getEngineConnectTimeoutMs());
        factory.setReadTimeout(engineProperties.getEngineReadTimeoutMs());
        return new RestTemplate(factory);
    }
}
