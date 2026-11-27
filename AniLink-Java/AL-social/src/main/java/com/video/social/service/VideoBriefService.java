package com.video.social.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.video.api.video.VideoInternalClient;
import com.video.api.video.dto.VideoBriefVo;
import com.video.api.video.dto.VideoStatsVo;
import com.video.social.vo.VideoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频摘要批量补全：收藏页的标题/封面/时长/统计统一来自 al-video 内部接口，禁止 JOIN video/video_stats。
 * 本地缓存 60s，空结果也缓存，防止不存在的 vid 反复穿透。
 */
@Slf4j
@Service
public class VideoBriefService {

    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ObjectProvider<VideoInternalClient> clientProvider;
    private final Cache<Long, Optional<VideoBriefVo>> briefCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(TTL)
            .build();
    private final Cache<Long, Optional<VideoStatsVo>> statsCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(TTL)
            .build();

    public VideoBriefService(ObjectProvider<VideoInternalClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    /** 批量取视频摘要；Feign 失败时降级为空结果，不阻断收藏列表 */
    public Map<Long, VideoBriefVo> byVids(Collection<Long> vids) {
        if (vids == null || vids.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinct = distinctIds(vids);
        List<Long> missing = distinct.stream().filter(id -> briefCache.getIfPresent(id) == null).toList();
        if (!missing.isEmpty()) {
            loadBriefs(missing);
        }
        Map<Long, VideoBriefVo> result = new HashMap<>();
        distinct.forEach(id -> {
            Optional<VideoBriefVo> cached = briefCache.getIfPresent(id);
            if (cached != null) {
                cached.ifPresent(video -> result.put(id, video));
            }
        });
        return result;
    }

    /**
     * 批量取视频统计：契约只提供单条接口，这里按 vid 走本地缓存，未命中才逐条回源。
     * 冷缓存下一页会放大约 20 次内部调用，属于迁移期可接受成本；后续可给契约补批量统计接口。
     */
    public Map<Long, VideoStatsVo> statsOf(Collection<Long> vids) {
        if (vids == null || vids.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinct = distinctIds(vids);
        Map<Long, VideoStatsVo> result = new HashMap<>();
        distinct.forEach(id -> {
            VideoStatsVo stats = statsOf(id);
            if (stats != null) {
                result.put(id, stats);
            }
        });
        return result;
    }

    /** 取单个视频统计，找不到返回 null */
    public VideoStatsVo statsOf(Long vid) {
        if (vid == null) {
            return null;
        }
        Optional<VideoStatsVo> cached = statsCache.getIfPresent(vid);
        if (cached == null) {
            cached = loadStats(vid);
        }
        return cached.orElse(null);
    }

    /** 按 vid 顺序补全遗留 VideoVo 列表；沿用旧 SQL「order by play」的升序行为 */
    public List<VideoVo> legacyVideosInPlayOrder(Collection<Long> vids) {
        List<VideoVo> result = new ArrayList<>();
        if (vids == null || vids.isEmpty()) {
            return result;
        }
        Map<Long, VideoBriefVo> videos = byVids(vids);
        Map<Long, VideoStatsVo> stats = statsOf(vids);
        vids.forEach(vid -> {
            VideoBriefVo brief = videos.get(vid);
            if (brief != null) {
                result.add(toLegacyVo(brief, stats.get(vid)));
            }
        });
        result.sort(Comparator.comparing(VideoVo::getPlay, Comparator.nullsFirst(Comparator.naturalOrder())));
        return result;
    }

    /** 视频摘要 + 统计合成旧 VideoVo 结构，字段名与拆分前保持一致 */
    public VideoVo toLegacyVo(VideoBriefVo brief, VideoStatsVo stats) {
        VideoVo vo = new VideoVo();
        vo.setVid(brief.getVid());
        vo.setUid(brief.getUid());
        vo.setTitle(brief.getTitle());
        vo.setDuration(brief.getDuration());
        vo.setCoverUrl(brief.getCoverUrl());
        vo.setCoverThumbUrl(brief.getCoverThumbUrl());
        vo.setUploadDate(brief.getUploadDate());
        vo.setPlay(brief.getPlay());
        if (stats != null) {
            vo.setPlay(stats.getPlay());
            vo.setDanmu(stats.getDanmu());
            vo.setGood(stats.getGood());
            vo.setBad(stats.getBad());
            vo.setCoin(stats.getCoin());
            vo.setCollect(stats.getCollect());
            vo.setShare(stats.getShare());
            vo.setComment(stats.getComment());
        }
        return vo;
    }

    private Set<Long> distinctIds(Collection<Long> vids) {
        return vids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** 批量回源视频摘要；先写空结果保证不存在的 id 命中负缓存 */
    private void loadBriefs(List<Long> missing) {
        try {
            List<VideoBriefVo> loaded = clientProvider.getObject().batch(missing);
            missing.forEach(id -> briefCache.put(id, Optional.empty()));
            if (loaded != null) {
                loaded.stream().filter(vo -> vo.getVid() != null)
                        .forEach(vo -> briefCache.put(vo.getVid(), Optional.of(vo)));
            }
        } catch (Exception e) {
            log.warn("批量取视频摘要失败，本轮降级为空：size={}", missing.size(), e);
        }
    }

    /** 单条回源视频统计，失败降级为空 */
    private Optional<VideoStatsVo> loadStats(Long vid) {
        Optional<VideoStatsVo> value;
        try {
            value = Optional.ofNullable(clientProvider.getObject().stats(vid));
        } catch (Exception e) {
            log.warn("取视频统计失败，本轮降级为空：vid={}", vid, e);
            value = Optional.empty();
        }
        statsCache.put(vid, value);
        return value;
    }
}
