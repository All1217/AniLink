package com.video.common.utils;

import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static com.video.common.constant.JWTConstant.*;

/**
 * JWT 签发与校验。
 *
 * <p>双令牌：access-token 短期（默认 30 分钟）用于访问业务接口；refresh-token 长期（默认 7 天）
 * 只用于换取新的 access-token。两者通过 {@code typ} claim 区分，防止刷新令牌被直接当访问令牌用。</p>
 *
 * <p>两个令牌都带 {@code jti}：refresh 的 jti 由服务端存 Redis 作为会话凭据；access 的 jti
 * 用于登出后的黑名单。</p>
 */
@Slf4j
public class JwtUtil {
    private static volatile SecretKey tokenSignKey =
            Keys.hmacShaKeyFor(DEFAULT_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    private static volatile long accessTtl = ACCESS_TOKEN_EXPIRATION;
    private static volatile long refreshTtl = REFRESH_TOKEN_EXPIRATION;
    /** 兼容期开关：允许没有 typ 的旧令牌按期望类型处理；关闭后旧令牌一律 602 */
    private static volatile boolean legacyCompat = true;

    private JwtUtil() {
    }

    /**
     * 由各服务启动时调用，注入签名密钥与有效期；未调用时使用 JWTConstant 中的默认值。
     */
    public static void init(String secret, long accessTtlMs, long refreshTtlMs) {
        init(secret, accessTtlMs, refreshTtlMs, legacyCompat);
    }

    public static void init(String secret, long accessTtlMs, long refreshTtlMs, boolean legacyCompatEnabled) {
        if (secret != null && !secret.isBlank()) {
            tokenSignKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        if (accessTtlMs > 0) {
            accessTtl = accessTtlMs;
        }
        if (refreshTtlMs > 0) {
            refreshTtl = refreshTtlMs;
        }
        legacyCompat = legacyCompatEnabled;
    }

    public static boolean isLegacyCompat() {
        return legacyCompat;
    }

    public static long accessTtlMillis() {
        return accessTtl;
    }

    public static long refreshTtlMillis() {
        return refreshTtl;
    }

    /** 签发访问令牌 */
    public static String createAccessToken(Long userId, String username) {
        return create(userId, username, TOKEN_TYPE_ACCESS, accessTtl);
    }

    /** 签发刷新令牌（jti 由调用方存入 Redis） */
    public static String createRefreshToken(Long userId) {
        return create(userId, String.valueOf(userId), TOKEN_TYPE_REFRESH, refreshTtl);
    }

    /** 兼容旧签名：等价于签发访问令牌 */
    public static String createToken(Long userId, String username) {
        return createAccessToken(userId, username);
    }

    private static String create(Long userId, String username, String type, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject("USER_INFO")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMillis))
                .claim(JWT_PAYLOAD_USER_ID, userId)
                .claim(JWT_PAYLOAD_USER_NAME, username)
                .claim(JWT_PAYLOAD_TYPE, type)
                .claim(JWT_PAYLOAD_JTI, UUID.randomUUID().toString())
                .signWith(tokenSignKey)
                .compact();
    }

    /** 解析并校验签名/过期，不校验类型（迁移期兼容旧 token） */
    public static Claims parseToken(String token) {
        return parseToken(token, null);
    }

    /**
     * 解析并校验签名/过期/类型。
     *
     * @param expectedType 期望的 typ；为 null 表示不校验。旧版 token 没有 typ，按兼容处理（仅告警）
     */
    public static Claims parseToken(String token, String expectedType) {
        if (token == null || token.isEmpty()) {
            throw new VideoException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        Claims claims;
        try {
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(tokenSignKey).build();
            Jws<Claims> claimsJws = jwtParser.parseClaimsJws(token);
            claims = claimsJws.getBody();
        } catch (ExpiredJwtException e) {
            throw new VideoException(ResultCodeEnum.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new VideoException(ResultCodeEnum.TOKEN_INVALID);
        }
        if (expectedType != null) {
            String type = claims.get(JWT_PAYLOAD_TYPE, String.class);
            if (type == null) {
                if (!legacyCompat) {
                    throw new VideoException(ResultCodeEnum.TOKEN_INVALID);
                }
                log.warn("旧版令牌缺少 typ，按 {} 处理；兼容期结束后将拒绝（al.auth.jwt.legacy-compat=false）", expectedType);
            } else if (!expectedType.equals(type)) {
                throw new VideoException(ResultCodeEnum.TOKEN_INVALID);
            }
        }
        return claims;
    }

    /** 取 jti（用于黑名单与会话吊销） */
    public static String jtiOf(Claims claims) {
        return claims.get(JWT_PAYLOAD_JTI, String.class);
    }

    public static void main(String[] args) {
        String access = createAccessToken(123123123L, "123123123");
        String refresh = createRefreshToken(123123123L);
        System.out.println("access = " + access);
        System.out.println("refresh = " + refresh);
        System.out.println("access claims = " + parseToken(access, TOKEN_TYPE_ACCESS));
        System.out.println("refresh claims = " + parseToken(refresh, TOKEN_TYPE_REFRESH));
    }
}
