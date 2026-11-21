package com.video.gateway.config;

import com.video.common.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 网关侧 JWT 初始化：与 AL-web 使用同一份密钥与有效期配置，
 * 否则会出现"网关判过期、AL-web 判合法"这种不一致。
 */
@Slf4j
@Configuration
public class JwtConfig {

    @Value("${al.auth.jwt.secret:}")
    private String secret;

    @Value("${al.auth.jwt.access-ttl:1800}")
    private long accessTtlSeconds;

    @Value("${al.auth.jwt.refresh-ttl:604800}")
    private long refreshTtlSeconds;

    @Value("${al.auth.jwt.legacy-compat:true}")
    private boolean legacyCompat;

    @PostConstruct
    public void init() {
        JwtUtil.init(secret.isBlank() ? null : secret, accessTtlSeconds * 1000L,
                refreshTtlSeconds * 1000L, legacyCompat);
        log.info("网关 JWT 初始化完成，access-ttl={}s，refresh-ttl={}s，旧令牌兼容={}，密钥来源={}",
                accessTtlSeconds, refreshTtlSeconds, legacyCompat, secret.isBlank() ? "默认常量" : "配置");
    }
}
