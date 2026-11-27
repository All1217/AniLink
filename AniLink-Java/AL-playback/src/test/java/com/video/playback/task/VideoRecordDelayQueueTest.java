package com.video.playback.task;

import com.video.model.entity.BrowseHistory;
import com.video.playback.mapper.HistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 播放进度延迟队列单测：覆盖窗口合并、到期落库、重复消费幂等、重启后任务仍在、旧任务失效。 */
@ExtendWith(MockitoExtension.class)
class VideoRecordDelayQueueTest {

    private static final long VID = 7L;
    private static final long UID = 42L;
    private static final long RECORD_ID = 100L;
    private static final String MEMBER = "7:42";
    private static final String PENDING_KEY = "video:record:pending:7";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private HistoryMapper historyMapper;

    private VideoRecordDelayQueue delayQueue;

    @BeforeEach
    void setUp() {
        delayQueue = new VideoRecordDelayQueue(redisTemplate, historyMapper);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("同一 10 秒窗口内多次上报：合并为一个延迟成员，只保留最新进度")
    void submitMergesWithinWindow() {
        delayQueue.submit(record(30.0));
        delayQueue.submit(record(66.0));

        // 同一个 member 被 ZADD 两次：窗口内只刷新 score，不产生第二条到期任务
        ArgumentCaptor<String> memberCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        verify(zSetOperations, org.mockito.Mockito.times(2))
                .add(eq(VideoRecordDelayQueue.DELAY_KEY), memberCaptor.capture(), scoreCaptor.capture());
        assertEquals(List.of(MEMBER, MEMBER), memberCaptor.getAllValues());
        assertTrue(scoreCaptor.getAllValues().get(0) < scoreCaptor.getAllValues().get(1),
                "第二次上报应把到期时间往后推");

        // 待落库载荷被覆盖为最后一次进度
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations, org.mockito.Mockito.times(2))
                .put(eq(PENDING_KEY), eq(String.valueOf(UID)), payloadCaptor.capture());
        String latestPayload = String.valueOf(payloadCaptor.getAllValues().get(1));
        assertEquals(66.0, com.video.common.utils.JsonUtils.parseObj(latestPayload).getDouble("moment"),
                0.0, "待落库载荷应是最新进度：" + latestPayload);

        // 每个窗口都重新续期 24 小时
        verify(redisTemplate, org.mockito.Mockito.times(2))
                .expire(eq(PENDING_KEY), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("到期任务落库：更新 bh_video 后 HDEL 待落库载荷 + ZREM 延迟成员")
    void consumePersistsDueTask() {
        long expireAt = 1_000L;
        stubDueTask(expireAt, 88.0);

        int persisted = delayQueue.consumeDueTasks();

        assertEquals(1, persisted);
        ArgumentCaptor<BrowseHistory> recordCaptor = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(historyMapper).updateById(recordCaptor.capture());
        assertEquals(RECORD_ID, recordCaptor.getValue().getId());
        assertEquals(88.0, recordCaptor.getValue().getDuration());
        verify(hashOperations).delete(PENDING_KEY, String.valueOf(UID));
        verify(zSetOperations).remove(VideoRecordDelayQueue.DELAY_KEY, MEMBER);
    }

    @Test
    @DisplayName("重复消费同一条任务：只做同值更新，不重复插入也不报错")
    void consumeIsIdempotentOnRepeatedConsumption() {
        stubDueTask(1_000L, 88.0);

        // 模拟两个实例拿到同一条到期任务，各自消费一次
        delayQueue.consumeDueTasks();
        delayQueue.consumeDueTasks();

        ArgumentCaptor<BrowseHistory> recordCaptor = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(historyMapper, org.mockito.Mockito.times(2)).updateById(recordCaptor.capture());
        BrowseHistory first = recordCaptor.getAllValues().get(0);
        BrowseHistory second = recordCaptor.getAllValues().get(1);
        assertEquals(first.getId(), second.getId());
        assertEquals(first.getDuration(), second.getDuration());
        verify(historyMapper, never()).insert(any(BrowseHistory.class));
    }

    @Test
    @DisplayName("进程重启后：JVM 内存没有任务，新实例仍能消费 Redis 中的到期任务")
    void restartedInstanceConsumesTaskFromRedis() {
        // 重启前任务已写入 Redis（ZSet 成员 + 待落库载荷），这里用 stub 模拟 Redis 中的存量数据
        stubDueTask(1_000L, 55.0);

        // 新实例没有任何 JVM 内存队列状态，状态全部来自 Redis
        VideoRecordDelayQueue restarted = new VideoRecordDelayQueue(redisTemplate, historyMapper);
        assertEquals(1, restarted.consumeDueTasks());

        verify(historyMapper).updateById(any(BrowseHistory.class));
        verify(zSetOperations).remove(VideoRecordDelayQueue.DELAY_KEY, MEMBER);
    }

    @Test
    @DisplayName("窗口内又有新上报时：旧任务被丢弃，不写库也不清理新任务")
    void staleTaskIsDiscarded() {
        long staleExpireAt = 1_000L;
        long latestExpireAt = 5_000L;
        when(zSetOperations.rangeByScoreWithScores(eq(VideoRecordDelayQueue.DELAY_KEY), eq(0D),
                anyDouble(), eq(0L), eq(200L)))
                .thenReturn(Set.<ZSetOperations.TypedTuple<String>>of(tuple(MEMBER, staleExpireAt)));
        // 载荷里的到期时间已被新上报刷新为 latestExpireAt，说明这条是旧任务
        when(hashOperations.get(PENDING_KEY, String.valueOf(UID)))
                .thenReturn(pendingJson(latestExpireAt, 99.0));

        assertEquals(0, delayQueue.consumeDueTasks());
        verify(historyMapper, never()).updateById(any(BrowseHistory.class));
        verify(hashOperations, never()).delete(anyString(), any());
        verify(zSetOperations, never()).remove(anyString(), anyString());
    }

    @Test
    @DisplayName("待落库载荷已被删除时：只摘除延迟成员，不写库")
    void missingPendingPayloadOnlyRemovesDelayMember() {
        when(zSetOperations.rangeByScoreWithScores(eq(VideoRecordDelayQueue.DELAY_KEY), eq(0D),
                anyDouble(), eq(0L), eq(200L)))
                .thenReturn(Set.<ZSetOperations.TypedTuple<String>>of(tuple(MEMBER, 1_000L)));
        when(hashOperations.get(PENDING_KEY, String.valueOf(UID))).thenReturn(null);

        assertEquals(0, delayQueue.consumeDueTasks());
        verify(historyMapper, never()).updateById(any(BrowseHistory.class));
        verify(zSetOperations).remove(VideoRecordDelayQueue.DELAY_KEY, MEMBER);
    }

    /** 构造一条上报记录 */
    private BrowseHistory record(double moment) {
        BrowseHistory record = new BrowseHistory();
        record.setId(RECORD_ID);
        record.setVid(VID);
        record.setUid(UID);
        record.setDuration(moment);
        return record;
    }

    /** 模拟 ZSet 里有一条到期任务、Hash 里有对应待落库载荷 */
    private void stubDueTask(long expireAt, double moment) {
        when(zSetOperations.rangeByScoreWithScores(eq(VideoRecordDelayQueue.DELAY_KEY), eq(0D),
                anyDouble(), eq(0L), eq(200L)))
                .thenReturn(Set.<ZSetOperations.TypedTuple<String>>of(tuple(MEMBER, expireAt)));
        when(hashOperations.get(PENDING_KEY, String.valueOf(UID)))
                .thenReturn(pendingJson(expireAt, moment));
    }

    /** 构造 ZSet 成员 + score */
    private ZSetOperations.TypedTuple<String> tuple(String member, long score) {
        return new DefaultTypedTuple<>(member, (double) score);
    }

    /** 构造待落库载荷 JSON */
    private String pendingJson(long expireAt, double moment) {
        return String.format("{\"id\":%d,\"moment\":%s,\"expireAt\":%d}", RECORD_ID, moment, expireAt);
    }
}
