package com.video.admin.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserRoleVo;
import com.video.common.exception.VideoException;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.common.result.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Optional;

/**
 * 管理接口的二次角色校验：网关只保证登录，这里要求 role>=1。
 * 用户表已归 al-user，角色通过内部接口读取并本地缓存 60s。
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final ObjectProvider<UserInternalClient> clientProvider;
    private final Cache<Long, Optional<Integer>> roleCache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofSeconds(60))
            .build();

    public AdminAuthInterceptor(ObjectProvider<UserInternalClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new VideoException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        Integer role = roleOf(loginUser.getUserId());
        if (role == null || role < 1) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        return true;
    }

    /** 角色查询失败按无权限处理，避免误放行；空结果也缓存，防止无效 uid 穿透 */
    private Integer roleOf(Long uid) {
        Optional<Integer> cached = roleCache.getIfPresent(uid);
        if (cached != null) {
            return cached.orElse(null);
        }
        try {
            UserRoleVo role = clientProvider.getObject().role(uid);
            Integer value = role == null ? null : role.getRole();
            roleCache.put(uid, Optional.ofNullable(value));
            return value;
        } catch (Exception e) {
            log.error("读取用户角色失败：uid={}", uid, e);
            return null;
        }
    }
}
