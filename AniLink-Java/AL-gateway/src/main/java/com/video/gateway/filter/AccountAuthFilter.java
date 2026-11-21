package com.video.gateway.filter;

import com.video.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.video.common.constant.JWTConstant.*;

/**
 * 网关鉴权：只认 access-token，且必须是 typ=access。
 *
 * <p>异常交由 {@code GatewayExceptionHandler} 翻译成 JSON：过期 601、非法/类型不匹配 602、
 * 缺少令牌 305（由下游 LoginAuthInterceptor 兜底时会再次返回 305）。</p>
 */
@Component
public class AccountAuthFilter implements GlobalFilter, Ordered {
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final Set<String> excludePath = new HashSet<>();

    {
        excludePath.add("/main/login/**");
        excludePath.add("/main/user/register");
        excludePath.add("/main/public/refresh");
        excludePath.add("/**/public/**");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1.获取请求request信息
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        // 2.判断是否是无需登录的路径
        if (isExcludePath(path)) {
            // 直接放行
            return chain.filter(exchange);
        }
        // 3.尝试获取token
        List<String> authHeaders = exchange.getRequest().getHeaders().get(AUTHORIZATION_HEADER);
        String token = authHeaders == null ? "" : authHeaders.get(0);
        // 4.校验签名、过期与令牌类型（refresh-token 不能当 access-token 用）
        Claims claims = JwtUtil.parseToken(token, TOKEN_TYPE_ACCESS);
        Long userId = claims.get(JWT_PAYLOAD_USER_ID, Long.class);
        // 5.如果用户是登录状态，尝试更新请求头，传递用户信息
        if (userId != null) {
            exchange.mutate()
                    .request(builder -> builder.header(USER_ID, userId.toString()))
                    .build();
        }
        // 6.放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isExcludePath(String antPath) {
        for (String pathPattern : this.excludePath) {
            if (antPathMatcher.match(pathPattern, antPath)) {
                return true;
            }
        }
        return false;
    }
}
