package com.video.common.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
@Data
@Accessors(chain = true, fluent = true)
public class CookieBuilder {
    private Charset charset = StandardCharsets.UTF_8;
    private int maxAge = -1;
    private String path = "/";
    private boolean httpOnly;
    /** 刷新令牌 Cookie 默认 Lax，阻断跨站携带 */
    private String sameSite = "Lax";
    /** HTTPS 部署时置为 true */
    private boolean secure;
    private String name;
    private String value;
    private String domain;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public CookieBuilder(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * 构建cookie，会对cookie值用UTF-8做URL编码，避免中文乱码
     */
    public void build() {
        if (response == null) {
            return;
        }
        // 用 ResponseCookie 构建，才能带上 SameSite（jakarta Cookie 不支持）；值仍按 UTF-8 URL 编码，保持与解码端兼容
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(name, URLEncoder.encode(value, charset))
                .httpOnly(httpOnly)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite);
        if (secure) {
            builder.secure(true);
        }
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        ResponseCookie responseCookie = builder.build();
        log.debug("生成cookie，编码方式:{}，【{}={}，domain:{};maxAge={};path={};httpOnly={};sameSite={};secure={}】",
                charset.name(), name, value, domain, maxAge, path, httpOnly, sameSite, secure);
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    /**
     * 利用UTF-8对cookie值解码，避免中文乱码问题
     *
     * @param cookieValue cookie原始值
     * @return 解码后的值
     */
    public String decode(String cookieValue) {
        return URLDecoder.decode(cookieValue, charset);
    }

    /**
     * 判断是否是 IP 地址
     */
    private boolean isIpAddress(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        // IPv4 简单判断
        return host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }
}
