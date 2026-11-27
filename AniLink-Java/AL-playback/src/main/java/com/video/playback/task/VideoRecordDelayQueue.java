package com.video.playback.task;

import com.video.common.utils.JsonUtils;
import com.video.common.utils.StringUtils;
import com.video.model.entity.BrowseHistory;
import com.video.playback.mapper.HistoryMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * 播放进度延迟落库队列（Redis 持久化，替代原 JVM DelayQueue）。
 *
 * <p>key 设计：
 * <ul>
 *   <li>video:record:delay：ZSet，member = vid:uid，score = 到期时间戳（毫秒）；</li>
 *   <li>video:record:pending:{vid}：Hash，field = uid，value = 待落库载荷 JSON，TTL 24 小时。</li>
 * </ul>
 *
 * <p>同一 (vid, uid) 在 10 秒静默窗口内反复上报时，只会覆盖同一个 hash field 并刷新同一个
 * ZSet member 的 score，窗口内天然合并为一次落库；任务数据全部在 Redis，进程重启或多实例
 * 部署都不会丢失，重复消费只是同值更新，幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoRecordDelayQueue {

    /** 延迟队列 ZSet key */
    public static final String DELAY_KEY = "video:record:delay";
    /** 待落库载荷 Hash key 模板（按 vid 分桶，field 为 uid） */
    public static final String PENDING_KEY_TEMPLATE = "video:record:pending:{}";
    /** 静默窗口：最后一次上报后 10 秒落库 */
    private static final long DELAY_MILLIS = Duration.ofSeconds(10).toMillis();
    /** 待落库载荷保留 24 小时，避免进程长时间停机后任务过期丢失 */
    private static final Duration PENDING_TTL = Duration.ofHours(24);
    /** 单轮最多消费的到期任务数 */
    private static final int BATCH_SIZE = 200;

    private final StringRedisTemplate redisTemplate;
    private final HistoryMapper historyMapper;

    /**
     * 提交/刷新一条待落库进度：HSET 覆盖最新进度 + ZADD 刷新到期时间。
     * 上报接口只调用本方法，不直接写数据库。
     */
    public void submit(BrowseHistory record) {
        if (record == null || record.getId() == null || record.getVid() == null || record.getUid() == null) {
            return;
        }
        String pendingKey = pendingKey(record.getVid());
        String member = member(record.getVid(), record.getUid());
        long expireAt = System.currentTimeMillis() + DELAY_MILLIS;
        try {
            // 1.覆盖写待落库载荷：同一窗口内只保留最后一次进度，并续期 24 小时
            PendingRecordData data = new PendingRecordData(record.getId(), record.getDuration(), expireAt);
            redisTemplate.opsForHash().put(pendingKey, record.getUid().toString(), JsonUtils.toJsonStr(data));
            redisTemplate.expire(pendingKey, PENDING_TTL);
            // 2.刷新到期时间：同一 member 更新 score，窗口随最后一次上报滑动
            redisTemplate.opsForZSet().add(DELAY_KEY, member, expireAt);
        } catch (Exception e) {
            log.warn("提交播放进度延迟任务失败，vid={}, uid={}", record.getVid(), record.getUid(), e);
        }
    }

    /** 删除待落库任务（删除历史时调用，避免延迟任务把刚删的记录「复活」） */
    public void remove(Long vid, Long uid) {
        if (vid == null || uid == null) {
            return;
        }
        try {
            String pendingKey = pendingKey(vid);
            redisTemplate.opsForZSet().remove(DELAY_KEY, member(vid, uid));
            redisTemplate.opsForHash().delete(pendingKey, uid.toString());
        } catch (Exception e) {
            log.warn("删除播放进度延迟任务失败，vid={}, uid={}", vid, uid, e);
        }
    }

    /** 定时消费入口：每秒扫描一轮到期任务 */
    @Scheduled(fixedDelay = 1000)
    public void scheduledConsume() {
        try {
            consumeDueTasks();
        } catch (Exception e) {
            log.error("播放进度延迟任务消费异常", e);
        }
    }

    /**
     * 消费一轮到期任务，返回本轮实际落库条数（单测直接调用，避免依赖调度器）。
     * 用 rangeByScoreWithScores 同时拿到到期成员与 score，用于识别已被新上报刷新的旧任务。
     */
    public int consumeDueTasks() {
        Set<ZSetOperations.TypedTuple<String>> dueTasks = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(DELAY_KEY, 0, System.currentTimeMillis(), 0, BATCH_SIZE);
        if (dueTasks == null || dueTasks.isEmpty()) {
            return 0;
        }
        int persisted = 0;
        for (ZSetOperations.TypedTuple<String> task : dueTasks) {
            try {
                if (consumeOne(task)) {
                    persisted++;
                }
            } catch (Exception e) {
                log.error("消费播放进度延迟任务失败，member={}", task == null ? null : task.getValue(), e);
            }
        }
        return persisted;
    }

    /**
     * 消费单条到期任务：
     * 1.读取待落库载荷，已不存在（被删除/已消费）就直接摘除延迟成员；
     * 2.score 与载荷里的到期时间不一致说明窗口内又有新上报，丢弃旧任务，等新任务到期再落库；
     * 3.按主键更新 bh_video 的播放进度（重复消费只是同值覆盖，幂等）；
     * 4.二次确认载荷没被新上报覆盖后再清理 Redis，避免误删新任务。
     */
    private boolean consumeOne(ZSetOperations.TypedTuple<String> task) {
        String member = task == null ? null : task.getValue();
        Long vid = parsePart(member, 0);
        Long uid = parsePart(member, 1);
        if (vid == null || uid == null) {
            redisTemplate.opsForZSet().remove(DELAY_KEY, member);
            return false;
        }
        String pendingKey = pendingKey(vid);
        String field = uid.toString();
        // 1.待落库载荷不存在：任务已失效，直接摘除延迟成员
        Object raw = redisTemplate.opsForHash().get(pendingKey, field);
        if (raw == null) {
            redisTemplate.opsForZSet().remove(DELAY_KEY, member);
            return false;
        }
        PendingRecordData data = JsonUtils.toBean(raw.toString(), PendingRecordData.class);
        if (data == null || data.getId() == null || data.getExpireAt() == null) {
            cleanup(pendingKey, field, member);
            return false;
        }
        // 2.窗口内又有新上报：ZSet score 已被刷新，本条旧任务丢弃，不写库也不删新载荷
        Double score = task.getScore();
        if (score != null && score.longValue() != data.getExpireAt()) {
            return false;
        }
        // 3.更新 bh_video 播放进度；行已被删除时 updateById 影响 0 行，同样安全
        BrowseHistory update = new BrowseHistory();
        update.setId(data.getId());
        update.setDuration(data.getMoment());
        historyMapper.updateById(update);
        // 4.落库期间若又有上报覆盖了载荷，保留新任务，交给下一轮
        Object latest = redisTemplate.opsForHash().get(pendingKey, field);
        if (latest == null || !latest.toString().equals(raw.toString())) {
            return true;
        }
        cleanup(pendingKey, field, member);
        return true;
    }

    /** 清理指定任务的待落库载荷与延迟成员 */
    private void cleanup(String pendingKey, String field, String member) {
        redisTemplate.opsForHash().delete(pendingKey, field);
        redisTemplate.opsForZSet().remove(DELAY_KEY, member);
    }

    /** 视频进度延迟队列的 member：vid:uid */
    private static String member(Long vid, Long uid) {
        return vid + ":" + uid;
    }

    /** 解析 member 的第 index 段为 Long，格式非法返回 null */
    private static Long parsePart(String member, int index) {
        if (member == null) {
            return null;
        }
        String[] parts = member.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            return Long.valueOf(parts[index]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String pendingKey(Long vid) {
        return StringUtils.format(PENDING_KEY_TEMPLATE, vid);
    }

    /** 待落库载荷：bh_video 主键 + 最新进度 + 本次到期时间（用于识别被刷新的旧任务） */
    @Data
    @NoArgsConstructor
    public static class PendingRecordData {
        private Long id;
        private Double moment;
        private Long expireAt;

        public PendingRecordData(Long id, Double moment, Long expireAt) {
            this.id = id;
            this.moment = moment;
            this.expireAt = expireAt;
        }
    }
}
