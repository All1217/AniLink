package com.video.common.constant;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
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

    /** 共享白名单 + 服务自定义路径；保证自定义配置不会覆盖掉登录/刷新等基础放行。 */
    public static List<String> withCustom(List<String> custom) {
        if (custom == null || custom.isEmpty()) {
            return EXCLUDE_PATTERNS;
        }
        List<String> merged = new ArrayList<>(EXCLUDE_PATTERNS);
        custom.stream().filter(pattern -> !merged.contains(pattern)).forEach(merged::add);
        return List.copyOf(merged);
    }
}
