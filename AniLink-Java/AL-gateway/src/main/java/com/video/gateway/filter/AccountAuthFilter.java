package com.video.gateway.filter;

import com.video.common.constant.AuthPaths;
import com.video.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.video.common.constant.JWTConstant.*;

/**
 * 网关鉴权：只认 access-token，且必须是 typ=access。
 *
 * <p>异常交由 {@code GatewayExceptionHandler} 翻译成 JSON：过期 601、非法/类型不匹配 602、
 * 缺少令牌 305（由下游 LoginAuthInterceptor 兜底时会再次返回 305）。</p>
 */
@Component
public class AccountAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求request信息
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        // 2.透传真实客户端 IP：登录/注册限流的 IP 维度要用，注意免登录路径也要带上
        String clientIp = resolveClientIp(request);
        // 2.1 清除客户端伪造的身份与 IP 头，只保留网关校验后的值
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.remove(USER_ID);
                    headers.remove("X-Real-IP");
                    headers.remove("X-Forwarded-For");
                }))
                .build();
        if (clientIp != null) {
            mutated = mutated.mutate().request(builder -> builder.header("X-Real-IP", clientIp)).build();
        }
        // 3.判断是否是无需登录的路径
        if (AuthPaths.isExcluded(path)) {
            // 直接放行
            return chain.filter(mutated);
        }
        // 4.尝试获取token
        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String token = authHeaders == null ? "" : authHeaders.get(0);
        // 5.校验签名、过期与令牌类型（refresh-token 不能当 access-token 用）
        Claims claims = JwtUtil.parseToken(token, TOKEN_TYPE_ACCESS);
        Long userId = claims.get(JWT_PAYLOAD_USER_ID, Long.class);
        // 6.如果用户是登录状态，尝试更新请求头，传递用户信息
        if (userId != null) {
            mutated = mutated.mutate()
                    .request(builder -> builder.header(USER_ID, userId.toString()))
                    .build();
        }
        // 7.放行
        return chain.filter(mutated);
    }

    /** 取客户端 IP：直连时以连接对端为准，避免客户端伪造 X-Forwarded-For 绕过限流 */
    private String resolveClientIp(ServerHttpRequest request) {
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
