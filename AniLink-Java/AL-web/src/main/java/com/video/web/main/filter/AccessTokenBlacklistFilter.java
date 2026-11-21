package com.video.web.main.filter;

import com.video.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.video.common.constant.JWTConstant.AUTH_BLACKLIST_PREFIX;
import static com.video.common.constant.JWTConstant.AUTHORIZATION_HEADER;

/**
 * 访问令牌黑名单校验：登出后把 access 的 jti 写入 Redis，这里逐请求检查，
 * 让"登出立即失效"不依赖于 access 自身的短期 TTL。
 *
 * <p>只做判黑，不做签名/类型的最终裁决（网关已做）；解析失败的令牌交给后续链路处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = JwtUtil.parseToken(token);
                String jti = JwtUtil.jtiOf(claims);
                if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(AUTH_BLACKLIST_PREFIX + jti))) {
                    reject(response);
                    return;
                }
            } catch (Exception e) {
                // 过期/非法令牌不在这里处理，避免与 Controller 的 601/602 语义重复
                log.debug("黑名单过滤器跳过无法解析的令牌：{}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":602,\"message\":\"令牌已失效，请重新登录\"}");
    }
}
