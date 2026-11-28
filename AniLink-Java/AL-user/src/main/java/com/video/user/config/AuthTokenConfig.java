package com.video.user.config;

import com.video.common.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启动时把配置注入 JwtUtil（签名密钥、access/refresh 有效期），并对外提供配置对象。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthTokenProperties.class)
public class AuthTokenConfig {

    private final AuthTokenProperties properties;

    @PostConstruct
    public void init() {
        JwtUtil.init(
                properties.getSecret().isBlank() ? null : properties.getSecret(),
                properties.getAccessTtl() * 1000L,
                properties.getRefreshTtl() * 1000L,
                properties.isLegacyCompat());
        log.info("AL-user JWT 初始化完成：access-ttl={}s，refresh-ttl={}s，宽限={}s，最大会话={}，旧令牌兼容={}",
                properties.getAccessTtl(), properties.getRefreshTtl(),
                properties.getPrevGrace(), properties.getMaxSessions(), properties.isLegacyCompat());
    }
}
