package com.video.playback.task;

import com.video.common.constant.Constant;
import com.video.common.utils.JsonUtils;
import com.video.common.utils.StringUtils;
import com.video.model.entity.BrowseHistory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 播放进度读缓存：video:record:{vid}（Hash，field=uid，TTL 60s）。
 * 只承担「下次上报时快速拿到记录 id」的作用，真正的待落库载荷在视频进度延迟队列里。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoRecordCache {

    /** 读缓存 1 分钟过期，上报期间持续命中，停止播放后自然失效 */
    private static final Duration TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    /** 写入读缓存；字段不全（如首次插入未拿到主键）时跳过，避免脏 key */
    public void write(BrowseHistory record) {
        if (record == null || record.getId() == null || record.getVid() == null || record.getUid() == null) {
            return;
        }
        try {
            String key = StringUtils.format(Constant.RECORD_KEY_TEMPLATE, record.getVid());
            redisTemplate.opsForHash().put(key, record.getUid().toString(),
                    JsonUtils.toJsonStr(new CacheData(record.getId(), record.getDuration())));
            redisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.warn("写入播放进度缓存失败，vid={}, uid={}", record.getVid(), record.getUid(), e);
        }
    }

    /** 读取读缓存：命中返回仅含 id 与进度节点的最小记录，否则返回 null */
    public BrowseHistory read(Long vid, Long uid) {
        if (vid == null || uid == null) {
            return null;
        }
        try {
            String key = StringUtils.format(Constant.RECORD_KEY_TEMPLATE, vid);
            Object cacheData = redisTemplate.opsForHash().get(key, uid.toString());
            if (cacheData == null) {
                return null;
            }
            CacheData data = JsonUtils.toBean(cacheData.toString(), CacheData.class);
            if (data == null || data.getId() == null) {
                return null;
            }
            BrowseHistory record = new BrowseHistory();
            record.setId(data.getId());
            record.setVid(vid);
            record.setUid(uid);
            record.setDuration(data.getMoment());
            return record;
        } catch (Exception e) {
            log.warn("读取播放进度缓存失败，vid={}, uid={}", vid, uid, e);
            return null;
        }
    }

    /** 删除读缓存（删除历史时调用） */
    public void clean(Long vid, Long uid) {
        if (vid == null || uid == null) {
            return;
        }
        try {
            String key = StringUtils.format(Constant.RECORD_KEY_TEMPLATE, vid);
            redisTemplate.opsForHash().delete(key, uid.toString());
        } catch (Exception e) {
            log.warn("清理播放进度缓存失败，vid={}, uid={}", vid, uid, e);
        }
    }

    /** 读缓存载荷：记录主键 + 上次观看节点 */
    @Data
    @NoArgsConstructor
    public static class CacheData {
        private Long id;
        private Double moment;

        public CacheData(Long id, Double moment) {
            this.id = id;
            this.moment = moment;
        }
    }
}
