package com.video.web.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 双令牌相关配置，前缀 al.auth.jwt。
 * 时间单位均为秒（TTL 与宽限窗口），便于 yml 里阅读。
 */
@Data
@ConfigurationProperties(prefix = "al.auth.jwt")
public class AuthTokenProperties {
    /** 签名密钥；为空时使用 JWTConstant 中的默认值，生产请用环境变量 AUTH_JWT_SECRET */
    private String secret = "";
    /** 访问令牌有效期，默认 30 分钟 */
    private long accessTtl = 1800;
    /** 刷新令牌有效期（滑动），默认 7 天 */
    private long refreshTtl = 604800;
    /** 轮换宽限窗口：同一旧 jti 在该窗口内重复使用返回同一份新令牌，默认 5 秒 */
    private long prevGrace = 5;
    /** 同一用户最多保留的会话数，超出按创建时间淘汰最旧 */
    private int maxSessions = 3;
    /** HTTPS 部署时置为 true，Cookie 会带 Secure */
    private boolean secureCookie = false;
    /** 刷新接口的来源白名单（留空表示不校验） */
    private List<String> allowedOrigins = new ArrayList<>();
    /** 兼容期：允许没有 typ 的旧令牌（默认开；关闭后旧令牌一律 602，所有人需重新登录一次） */
    private boolean legacyCompat = true;
}
