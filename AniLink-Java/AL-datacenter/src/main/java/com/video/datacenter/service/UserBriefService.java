package com.video.datacenter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserBriefVo;
import com.video.model.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户摘要批量补全：推荐/相似用户接口统一走这里，避免逐条 Feign 放大。
 * 本地缓存 60s，空结果也缓存，防止不存在的 id 反复穿透；al-user 不可用时降级为空。
 */
@Slf4j
@Service
public class UserBriefService {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final UserInternalClient userInternalClient;
    private final Cache<Long, Optional<UserBriefVo>> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(TTL)
            .build();

    public UserBriefService(UserInternalClient userInternalClient) {
        this.userInternalClient = userInternalClient;
    }

    /** 批量取用户摘要；Feign 失败时降级为空结果，不阻断主流程 */
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

    /** 相似用户列表：保持引擎返回顺序，字段与旧 UserInfo 返回保持兼容 */
    public List<UserInfo> similarUsers(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        Map<Long, UserBriefVo> users = byUids(uids);
        List<UserInfo> result = new ArrayList<>();
        uids.forEach(id -> {
            UserBriefVo brief = users.get(id);
            if (brief == null) {
                return;
            }
            UserInfo user = new UserInfo();
            user.setUid(brief.getUid());
            user.setNickname(brief.getNickname());
            user.setAvatar(brief.getAvatar());
            user.setDescription(brief.getDescription());
            user.setStatus(brief.getStatus());
            result.add(user);
        });
        return result;
    }

    /** 批量回源 al-user；异常时降级为空，避免推荐/标签接口 500 */
    private void load(List<Long> missing) {
        try {
            List<UserBriefVo> loaded = userInternalClient.batch(missing);
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
