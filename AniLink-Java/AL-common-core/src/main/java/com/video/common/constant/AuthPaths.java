package com.video.common.constant;

import org.springframework.util.AntPathMatcher;

import java.util.List;

/** 网关与资源服务共用的免登录路径。 */
public final class AuthPaths {

    public static final List<String> EXCLUDE_PATTERNS = List.of(
            "/main/login/**",
            "/main/user/register",
            "/main/public/refresh",
            "/**/public/**"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private AuthPaths() {
    }

    public static boolean isExcluded(String path) {
        return EXCLUDE_PATTERNS.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }
}
