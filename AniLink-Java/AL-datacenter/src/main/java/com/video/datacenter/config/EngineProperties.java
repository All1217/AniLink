package com.video.datacenter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 数据中心配置：Flask 算法引擎地址与超时（Flask 不可达时接口降级为空结果）。 */
@Data
@Component
@ConfigurationProperties(prefix = "al.datacenter")
public class EngineProperties {

    /** 算法引擎基础地址，默认 http://localhost:5000 */
    private String engineUrl = "http://localhost:5000";

    /** 连接超时（毫秒） */
    private int engineConnectTimeoutMs = 1000;

    /** 读取超时（毫秒） */
    private int engineReadTimeoutMs = 3000;
}
