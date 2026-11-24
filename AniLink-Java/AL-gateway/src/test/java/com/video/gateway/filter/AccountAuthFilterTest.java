package com.video.gateway.filter;

import com.video.common.constant.JWTConstant;
import com.video.common.exception.VideoException;
import com.video.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AccountAuthFilterTest {

    private final AccountAuthFilter filter = new AccountAuthFilter();

    @BeforeAll
    static void initJwt() {
        JwtUtil.init("0123456789abcdef0123456789abcdef", 1_800_000, 604_800_000, true);
    }

    @Test
    void injectsUserIdAndClientIp() {
        String token = JwtUtil.createAccessToken(42L, "42");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, token)
                        .header("X-Forwarded-For", "10.0.0.8, 10.0.0.1")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertNotSame(exchange, forwarded.get());
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
        assertEquals("10.0.0.8", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
    }

    @Test
    void passesPublicPathWithoutTokenAndForwardsIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/public/refresh")
                        .header("X-Forwarded-For", "10.0.0.9")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertNotSame(exchange, forwarded.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
        assertEquals("10.0.0.9", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
    }

    @Test
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo").build());

        assertThrows(VideoException.class, () -> filter.filter(exchange, ex -> Mono.empty()));
    }

    @Test
    void rejectsRefreshTokenUsedAsAccessToken() {
        String refreshToken = JwtUtil.createRefreshToken(42L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, refreshToken)
                        .build());

        assertThrows(VideoException.class, () -> filter.filter(exchange, ex -> Mono.empty()));
    }
}
