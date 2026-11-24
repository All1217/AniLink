package com.video.gateway.filter;

import com.video.common.constant.JWTConstant;
import com.video.common.exception.VideoException;
import com.video.common.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AccountAuthFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final AccountAuthFilter filter = new AccountAuthFilter();

    @BeforeEach
    void initJwt() {
        JwtUtil.init(SECRET, 1_800_000, 604_800_000, true);
    }

    @AfterEach
    void resetJwt() {
        JwtUtil.init(SECRET, 1_800_000, 604_800_000, true);
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
    void stripsClientSuppliedIdentityHeaders() {
        String token = JwtUtil.createAccessToken(42L, "42");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, token)
                        .header(JWTConstant.USER_ID, "999")
                        .header("X-Real-IP", "10.9.9.9")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
        assertNotEquals("10.9.9.9", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
    }

    @Test
    void publicPathDoesNotTrustClientUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/public/refresh")
                        .header(JWTConstant.USER_ID, "999")
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertNull(forwarded.get().getRequest().getHeaders().getFirst(JWTConstant.USER_ID));
    }

    @Test
    void usesRemoteAddressOverForwardedFor() {
        String token = JwtUtil.createAccessToken(42L, "42");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, token)
                        .header("X-Forwarded-For", "10.0.0.8")
                        .remoteAddress(new InetSocketAddress("10.1.1.1", 5000))
                        .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertEquals("10.1.1.1", forwarded.get().getRequest().getHeaders().getFirst("X-Real-IP"));
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("X-Forwarded-For"));
    }

    @Test
    void rejectsMissingTokenWith305() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        VideoException ex = assertThrows(VideoException.class, () -> filter.filter(exchange, e -> {
            chainCalled.set(true);
            return Mono.empty();
        }));

        assertEquals(305, ex.getCode());
        assertFalse(chainCalled.get());
    }

    @Test
    void rejectsRefreshTokenUsedAsAccessTokenWith602() {
        String refreshToken = JwtUtil.createRefreshToken(42L);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, refreshToken)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        VideoException ex = assertThrows(VideoException.class, () -> filter.filter(exchange, e -> {
            chainCalled.set(true);
            return Mono.empty();
        }));

        assertEquals(602, ex.getCode());
        assertFalse(chainCalled.get());
    }

    @Test
    void rejectsGarbageTokenWith602() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, "not-a-jwt")
                        .build());

        VideoException ex = assertThrows(VideoException.class,
                () -> filter.filter(exchange, e -> Mono.empty()));

        assertEquals(602, ex.getCode());
    }

    @Test
    void rejectsExpiredTokenWith601() throws InterruptedException {
        JwtUtil.init(SECRET, 1, 604_800_000, true);
        String token = JwtUtil.createAccessToken(42L, "42");
        Thread.sleep(1_100);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/main/video/pageVideo")
                        .header(JWTConstant.AUTHORIZATION_HEADER, token)
                        .build());

        VideoException ex = assertThrows(VideoException.class,
                () -> filter.filter(exchange, e -> Mono.empty()));

        assertEquals(601, ex.getCode());
    }
}
