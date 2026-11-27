package com.video.playback.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserBriefVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户摘要批量补全：历史页作者昵称统一走这里，避免逐条 Feign 放大。
 * 本地缓存 60s，空结果也缓存，防止不存在的 id 反复穿透。
 */
@Slf4j
@Service
public class UserBriefService {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ObjectProvider<UserInternalClient> clientProvider;
    private final Cache<Long, Optional<UserBriefVo>> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(TTL)
            .build();

    public UserBriefService(ObjectProvider<UserInternalClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /** 批量取用户摘要；Feign 失败时降级为空结果，不阻断历史列表 */
    public Map<Long, UserBriefVo> byUids(Collection<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinct = uids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> missing = distinct.stream().filter(id -> cache.getIfPresent(id) == null).toList();
        if (!missing.isEmpty()) {
            load(missing);
        }
        Map<Long, UserBriefVo> result = new HashMap<>();
        distinct.forEach(id -> {
            Optional<UserBriefVo> cached = cache.getIfPresent(id);
            if (cached != null) {
                cached.ifPresent(user -> result.put(id, user));
            }
        });
        return result;
    }

    /** 延迟到首次调用再创建 Feign 代理，避免启动期循环依赖 */
    private void load(List<Long> missing) {
        try {
            List<UserBriefVo> loaded = clientProvider.getObject().batch(missing);
            // 先写空结果，保证不存在的 id 命中负缓存
            missing.forEach(id -> cache.put(id, Optional.empty()));
            if (loaded != null) {
                loaded.stream().filter(vo -> vo.getUid() != null)
                        .forEach(vo -> cache.put(vo.getUid(), Optional.of(vo)));
            }
        } catch (Exception e) {
            log.warn("批量取用户摘要失败，本轮降级为空：size={}", missing.size(), e);
        }
    }
}
