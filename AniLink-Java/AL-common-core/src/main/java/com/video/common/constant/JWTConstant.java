package com.video.common.constant;

public class JWTConstant {
    /** 访问令牌请求头 */
    public static final String AUTHORIZATION_HEADER = "access-token";
    /** 刷新令牌 Cookie 名 */
    public static final String COOKIE_HEADER = "refresh-token";
    /** 网关透传给下游的用户头 */
    public static final String USER_ID = "user-id";

    public static final String JWT_PAYLOAD_USER_ID = "userId";
    public static final String JWT_PAYLOAD_JTI = "jti";
    public static final String JWT_PAYLOAD_USER_NAME = "username";
    /** 令牌类型：access / refresh，用于防止刷新令牌被当成访问令牌使用 */
    public static final String JWT_PAYLOAD_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** 旧版 jti 键，仅迁移期读取（jwt:uid:{uid} -> jti） */
    public static final String JWT_REDIS_KEY_PREFIX = "jwt:uid:";
    /** 有效刷新会话：auth:rt:{jti} -> uid，TTL 即刷新令牌有效期（每次续约重置） */
    public static final String AUTH_REFRESH_KEY_PREFIX = "auth:rt:";
    /** 用户全部有效会话：auth:uid:{uid} -> Set<jti>，用于多端与全端吊销 */
    public static final String AUTH_USER_KEY_PREFIX = "auth:uid:";
    /** 轮换宽限窗口：auth:rt:prev:{oldJti} -> {access, jti}，避免并发刷新互相踢下线 */
    public static final String AUTH_REFRESH_PREV_PREFIX = "auth:rt:prev:";
    /** 登出后的访问令牌黑名单：auth:black:{jti} */
    public static final String AUTH_BLACKLIST_PREFIX = "auth:black:";

    /** 默认访问令牌有效期：30 分钟（可被 al.auth.jwt.access-ttl 覆盖） */
    public static final long ACCESS_TOKEN_EXPIRATION = 30 * 60 * 1000L;
    /** 默认刷新令牌有效期：7 天（滑动，可被 al.auth.jwt.refresh-ttl 覆盖） */
    public static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;
    /** 刷新令牌有效期（天），用于 Cookie maxAge */
    public static final int REFRESH_TOKEN_EXPIRATION_DAY = 7;

    /**
     * 默认签名密钥（仅开发兜底）。
     *
     * <p>生产必须通过 al.auth.jwt.secret / 环境变量 AUTH_JWT_SECRET 覆盖，且：</p>
     * <ul>
     *   <li>不少于 32 字节（HS256 要求 256bit，否则 Keys.hmacShaKeyFor 抛 WeakKeyException，服务启动失败）；</li>
     *   <li>与 AL-gateway 配置的值完全一致；</li>
     *   <li>随机且不入库；更换后所有已签发令牌立即失效。</li>
     * </ul>
     */
    public static final String DEFAULT_JWT_SECRET = "M0PKKI6pYGVWWfDZw90a0lTpGYX1d4AQ";
}
