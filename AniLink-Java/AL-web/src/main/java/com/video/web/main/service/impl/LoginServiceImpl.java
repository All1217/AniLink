package com.video.web.main.service.impl;

import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import com.video.common.utils.JwtUtil;
import com.video.common.utils.PasswordUtil;
import com.video.common.utils.WebUtils;
import com.video.model.entity.UserInfo;
import com.video.model.enums.BaseStatus;
import com.video.web.main.config.AuthTokenProperties;
import com.video.web.main.mapper.UserInfoMapper;
import com.video.web.main.service.LoginService;
import com.video.web.main.util.AccountRules;
import com.video.web.main.vo.ChangePasswordVo;
import com.video.web.main.vo.LoginVo;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.video.common.constant.JWTConstant.*;

/**
 * 双令牌登录：
 * <ul>
 *   <li>access-token（默认 30 分钟）随响应体返回，前端放请求头；</li>
 *   <li>refresh-token（默认 7 天，滑动）只放 HttpOnly Cookie，仅用于换取新 access；</li>
 *   <li>每次续约轮换 refresh 并重置 TTL，旧 refresh 5 秒内重复使用返回同一份新令牌（并发/重试幂等）；</li>
 *   <li>旧 refresh 超窗再用视为泄露，吊销该用户全部会话。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {
    private final UserInfoMapper userInfoMapper;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final AuthTokenProperties tokenProperties;

    /** 登录失败计数：账号维度 */
    private static final String LOGIN_FAIL_USER_PREFIX = "auth:login:fail:user:";
    /** 登录失败计数：IP 维度（网关透传 X-Real-IP） */
    private static final String LOGIN_FAIL_IP_PREFIX = "auth:login:fail:ip:";

    @Value("${al.auth.login.max-fail-per-user:5}")
    private int maxFailPerUser;

    @Value("${al.auth.login.max-fail-per-ip:20}")
    private int maxFailPerIp;

    @Value("${al.auth.login.window-seconds:900}")
    private long failWindowSeconds;

    @Override
    public String login(LoginVo loginVo) {
        String username = AccountRules.normalizeUsername(loginVo.getUsername());
        String rawPassword = loginVo.getPassword();
        if (username.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "请输入用户名和密码");
        }
        String ip = WebUtils.getHeader("X-Real-IP");
        // 1.限流：账号维度和 IP 维度都超阈值就拒绝，避免撞库
        checkLoginRate(username, ip);
        // 2.查用户；兼容历史"数字 uid 当账号"的老用法
        UserInfo user = userInfoMapper.selectOneByUsername(username);
        if (user == null && username.chars().allMatch(Character::isDigit)) {
            try {
                user = userInfoMapper.getById(Long.parseLong(username));
            } catch (NumberFormatException ignored) {
                user = null;
            }
        }
        // 3.用户不存在时也跑一次 bcrypt，抹平响应时间差（防账号枚举）
        if (user == null) {
            PasswordUtil.dummyVerify(rawPassword);
            recordLoginFailure(username, ip);
            throw new VideoException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
        }
        // 4.密码校验；失败统一文案，不区分"账号不存在/密码错误"
        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            recordLoginFailure(username, ip);
            throw new VideoException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
        }
        // 5.历史 MD5 就地升级为 bcrypt
        if (PasswordUtil.needsUpgrade(user.getPassword())) {
            userInfoMapper.updatePassword(user.getUid(), PasswordUtil.encode(rawPassword));
            log.info("用户 {} 的密码哈希已升级为 bcrypt", user.getUid());
        }
        // 6.状态校验放在密码正确之后，避免泄露封禁状态
        if (Objects.equals(user.getStatus(), BaseStatus.DISABLE.getCode())) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCOUNT_DISABLED_ERROR);
        } else if (Objects.equals(user.getStatus(), BaseStatus.LOGOUT.getCode())) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCOUNT_LOGOUT_ERROR);
        }
        // 7.重置失败计数、触发画像、签发令牌
        clearLoginFailures(username, ip);
        notifyProfile(user.getUid());
        return issueTokens(user.getUid(), user.getUsername());
    }

    @Override
    public String issueTokens(Long uid, String username) {
        String access = JwtUtil.createAccessToken(uid, username);
        String refresh = JwtUtil.createRefreshToken(uid);
        String jti = JwtUtil.jtiOf(JwtUtil.parseToken(refresh, TOKEN_TYPE_REFRESH));
        saveSession(uid, jti, JwtUtil.refreshTtlMillis());
        writeRefreshCookie(refresh);
        return access;
    }

    @Override
    public void changePassword(Long uid, ChangePasswordVo changePasswordVo) {
        UserInfo user = userInfoMapper.getById(uid);
        if (user == null) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "用户不存在");
        }
        // 1.必须校验旧密码（OWASP：改密要先证明是本人）
        if (!PasswordUtil.matches(changePasswordVo.getOldPassword(), user.getPassword())) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "当前密码不正确");
        }
        // 2.新密码规则校验
        AccountRules.validatePassword(changePasswordVo.getNewPassword());
        if (Objects.equals(changePasswordVo.getOldPassword(), changePasswordVo.getNewPassword())) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "新密码不能与当前密码相同");
        }
        // 3.更新哈希并吊销全部会话，强制所有端重新登录
        userInfoMapper.updatePassword(uid, PasswordUtil.encode(changePasswordVo.getNewPassword()));
        revokeAll(uid);
        // 当前请求携带的 access 也立刻拉黑，避免改密后旧令牌还能用满 30 分钟
        blacklistAccessToken(WebUtils.getHeader(AUTHORIZATION_HEADER));
        log.info("用户 {} 修改密码成功，已吊销全部会话", uid);
    }

    /** 登录限流检查：账号 5 次/15 分钟、IP 20 次/15 分钟 */
    private void checkLoginRate(String username, String ip) {
        if (readFailCount(LOGIN_FAIL_USER_PREFIX + username.toLowerCase()) >= maxFailPerUser) {
            throw new VideoException(ResultCodeEnum.TOO_MANY_ATTEMPTS);
        }
        if (ip != null && !ip.isBlank() && readFailCount(LOGIN_FAIL_IP_PREFIX + ip) >= maxFailPerIp) {
            throw new VideoException(ResultCodeEnum.TOO_MANY_ATTEMPTS);
        }
    }

    private long readFailCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void recordLoginFailure(String username, String ip) {
        increaseFailCount(LOGIN_FAIL_USER_PREFIX + username.toLowerCase());
        if (ip != null && !ip.isBlank()) {
            increaseFailCount(LOGIN_FAIL_IP_PREFIX + ip);
        }
    }

    private void increaseFailCount(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(failWindowSeconds));
        }
    }

    private void clearLoginFailures(String username, String ip) {
        redisTemplate.delete(LOGIN_FAIL_USER_PREFIX + username.toLowerCase());
        if (ip != null && !ip.isBlank()) {
            redisTemplate.delete(LOGIN_FAIL_IP_PREFIX + ip);
        }
    }

    @Override
    public UserInfo getLoginUserInfo(Long userId) {
        return userInfoMapper.getById(userId);
    }

    @Override
    public String refreshToken(String refreshToken) {
        // 1.校验签名/过期/类型（必须是 refresh）
        Claims claims = JwtUtil.parseToken(refreshToken, TOKEN_TYPE_REFRESH);
        Long userId = claims.get(JWT_PAYLOAD_USER_ID, Long.class);
        String jti = JwtUtil.jtiOf(claims);
        if (userId == null || jti == null) {
            throw new VideoException(ResultCodeEnum.TOKEN_INVALID);
        }
        // 2.宽限窗口：同一旧 refresh 在窗口内重复请求，返回上次生成的那份令牌（不再轮换）
        String cached = redisTemplate.opsForValue().get(AUTH_REFRESH_PREV_PREFIX + jti);
        if (cached != null) {
            String[] pair = cached.split("\\|", 2);
            if (pair.length == 2) {
                writeRefreshCookie(pair[1]);
                return pair[0];
            }
        }
        // 3.当前会话必须存在，否则视为重用/已失效
        String owner = redisTemplate.opsForValue().get(AUTH_REFRESH_KEY_PREFIX + jti);
        if (!String.valueOf(userId).equals(owner)) {
            log.warn("refresh-token 重用或已失效，吊销该用户全部会话，uid={}", userId);
            revokeAll(userId);
            throw new VideoException(ResultCodeEnum.TOKEN_INVALID);
        }
        // 4.轮换：删除旧会话，签发新的一对令牌并重置 TTL（滑动过期）
        redisTemplate.delete(AUTH_REFRESH_KEY_PREFIX + jti);
        redisTemplate.opsForZSet().remove(AUTH_USER_KEY_PREFIX + userId, jti);

        String access = JwtUtil.createAccessToken(userId, String.valueOf(userId));
        String newRefresh = JwtUtil.createRefreshToken(userId);
        String newJti = JwtUtil.jtiOf(JwtUtil.parseToken(newRefresh, TOKEN_TYPE_REFRESH));
        saveSession(userId, newJti, JwtUtil.refreshTtlMillis());

        // 5.写入宽限缓存，供并发/重试复用同一份新令牌
        Duration grace = Duration.ofSeconds(Math.max(1, tokenProperties.getPrevGrace()));
        redisTemplate.opsForValue().set(AUTH_REFRESH_PREV_PREFIX + jti, access + "|" + newRefresh, grace);

        writeRefreshCookie(newRefresh);
        notifyProfile(userId);
        return access;
    }

    @Override
    public void logout() {
        String refreshToken = readRefreshCookie();
        String accessToken = WebUtils.getHeader(AUTHORIZATION_HEADER);
        // 1.吊销刷新会话
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = JwtUtil.parseToken(refreshToken, TOKEN_TYPE_REFRESH);
                Long uid = claims.get(JWT_PAYLOAD_USER_ID, Long.class);
                String jti = JwtUtil.jtiOf(claims);
                if (uid != null && jti != null) {
                    redisTemplate.delete(AUTH_REFRESH_KEY_PREFIX + jti);
                    redisTemplate.opsForZSet().remove(AUTH_USER_KEY_PREFIX + uid, jti);
                }
            } catch (Exception e) {
                log.debug("登出时 refresh-token 已失效：{}", e.getMessage());
            }
        }
        // 2.把当前 access 加入黑名单，剩余有效期内立即失效
        blacklistAccessToken(accessToken);
        // 3.清 Cookie
        writeRefreshCookie("");
    }

    /** 会话序号：ZSet 的 score 用单调递增数，避免同毫秒登录时创建时间相同导致淘汰顺序错乱 */
    private static final java.util.concurrent.atomic.AtomicLong SESSION_SEQ =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() * 1000L);

    /** 登记会话：auth:rt:{jti}=uid，auth:uid:{uid} 记录创建序号用于淘汰最旧，TTL 与 refresh 一致 */
    private void saveSession(Long uid, String jti, long ttlMillis) {
        Duration ttl = Duration.ofMillis(ttlMillis);
        redisTemplate.opsForValue().set(AUTH_REFRESH_KEY_PREFIX + jti, uid.toString(), ttl);
        String userKey = AUTH_USER_KEY_PREFIX + uid;
        redisTemplate.opsForZSet().add(userKey, jti, SESSION_SEQ.incrementAndGet());
        redisTemplate.expire(userKey, ttl);
        evictOldestSessions(userKey);
    }

    /** 多端上限：超出 max-sessions 时淘汰创建时间最早的会话 */
    private void evictOldestSessions(String userKey) {
        Long size = redisTemplate.opsForZSet().zCard(userKey);
        while (size != null && size > tokenProperties.getMaxSessions()) {
            Set<String> oldest = redisTemplate.opsForZSet().range(userKey, 0, 0);
            if (oldest == null || oldest.isEmpty()) {
                return;
            }
            String evicted = oldest.iterator().next();
            redisTemplate.delete(AUTH_REFRESH_KEY_PREFIX + evicted);
            redisTemplate.opsForZSet().remove(userKey, evicted);
            size = redisTemplate.opsForZSet().zCard(userKey);
        }
    }

    /** 吊销该用户全部会话（重用检测触发） */
    private void revokeAll(Long uid) {
        String userKey = AUTH_USER_KEY_PREFIX + uid;
        Set<String> jtis = redisTemplate.opsForZSet().range(userKey, 0, -1);
        if (jtis != null) {
            jtis.forEach(jti -> redisTemplate.delete(AUTH_REFRESH_KEY_PREFIX + jti));
        }
        redisTemplate.delete(userKey);
    }

    /** 访问令牌加入黑名单，TTL 取剩余有效期，避免无意义的长缓存 */
    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            Claims claims = JwtUtil.parseToken(accessToken);
            String jti = JwtUtil.jtiOf(claims);
            Date expiration = claims.getExpiration();
            long remain = expiration == null ? JwtUtil.accessTtlMillis() : expiration.getTime() - System.currentTimeMillis();
            if (jti != null && remain > 0) {
                redisTemplate.opsForValue().set(AUTH_BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(remain));
            }
        } catch (Exception e) {
            log.debug("登出时 access-token 已失效，跳过黑名单：{}", e.getMessage());
        }
    }

    /** 从请求 Cookie 里读取 refresh-token（值经过 URL 编码） */
    private String readRefreshCookie() {
        HttpServletRequest request = WebUtils.getRequest();
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_HEADER.equals(cookie.getName())) {
                return WebUtils.cookieBuilder().decode(cookie.getValue());
            }
        }
        return null;
    }

    /** 写/清 refresh-token Cookie：HttpOnly + SameSite=Lax，TTL 跟随刷新令牌 */
    private void writeRefreshCookie(String refreshToken) {
        boolean clear = refreshToken == null || refreshToken.isEmpty();
        WebUtils.cookieBuilder()
                .name(COOKIE_HEADER)
                .value(clear ? "" : refreshToken)
                .maxAge(clear ? 0 : Math.toIntExact(tokenProperties.getRefreshTtl()))
                .httpOnly(true)
                .secure(tokenProperties.isSecureCookie())
                .sameSite("Lax")
                .build();
    }

    /** 通知画像服务（沿用原逻辑：python 侧无法解析 Long，这里统一转字符串）；MQ 故障不应影响登录 */
    private void notifyProfile(Long uid) {
        try {
            Map<String, Object> msg = new HashMap<>(2);
            msg.put("uid", uid.toString());
            msg.put("timestamp", System.currentTimeMillis());
            rabbitTemplate.convertAndSend("userProfile.exchange", "userProfile", msg);
        } catch (Exception e) {
            log.warn("发送用户画像消息失败（不影响登录）：{}", e.getMessage());
        }
    }
}
