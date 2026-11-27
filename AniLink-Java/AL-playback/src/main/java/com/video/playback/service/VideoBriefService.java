package com.video.playback.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.video.api.video.VideoInternalClient;
import com.video.api.video.dto.VideoBriefVo;
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
 * 视频摘要批量补全：历史页标题/封面/时长/作者来自 al-video 内部接口，禁止 JOIN video 表。
 * 本地缓存 60s，空结果也缓存，防止不存在的 vid 反复穿透。
 */
@Slf4j
@Service
public class VideoBriefService {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ObjectProvider<VideoInternalClient> clientProvider;
    private final Cache<Long, Optional<VideoBriefVo>> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(TTL)
            .build();

    public VideoBriefService(ObjectProvider<VideoInternalClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /** 批量取视频摘要；Feign 失败时降级为空结果，不阻断历史列表 */
    public Map<Long, VideoBriefVo> byVids(Collection<Long> vids) {
        if (vids == null || vids.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinct = vids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> missing = distinct.stream().filter(id -> cache.getIfPresent(id) == null).toList();
        if (!missing.isEmpty()) {
            load(missing);
        }
        Map<Long, VideoBriefVo> result = new HashMap<>();
        distinct.forEach(id -> {
            Optional<VideoBriefVo> cached = cache.getIfPresent(id);
            if (cached != null) {
                cached.ifPresent(video -> result.put(id, video));
            }
        });
        return result;
    }

    /** 延迟到首次调用再创建 Feign 代理，避免启动期循环依赖 */
    private void load(List<Long> missing) {
        try {
            List<VideoBriefVo> loaded = clientProvider.getObject().batch(missing);
            // 先写空结果，保证不存在的 vid 命中负缓存
            missing.forEach(id -> cache.put(id, Optional.empty()));
            if (loaded != null) {
                loaded.stream().filter(vo -> vo.getVid() != null)
                        .forEach(vo -> cache.put(vo.getVid(), Optional.of(vo)));
            }
        } catch (Exception e) {
            log.warn("批量取视频摘要失败，本轮降级为空：size={}", missing.size(), e);
        }
    }
}
