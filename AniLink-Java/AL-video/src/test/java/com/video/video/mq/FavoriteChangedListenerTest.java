package com.video.video.mq;

import com.video.model.dto.social.FavoriteChangedDTO;
import com.video.video.service.FavoriteCollectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 收藏事件消费：eventId 去重、失败释放去重键、缺失 eventId 丢弃。 */
@ExtendWith(MockitoExtension.class)
class FavoriteChangedListenerTest {

    private static final String DEDUP_KEY = "social:favorite:changed:event:e1";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private FavoriteCollectService favoriteCollectService;
    @InjectMocks
    private FavoriteChangedListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void firstDeliveryAppliesEvent() {
        when(valueOperations.setIfAbsent(eq(DEDUP_KEY), eq("1"), any(Duration.class))).thenReturn(true);

        listener.listenFavoriteChanged(event("e1"));

        verify(favoriteCollectService).apply(any(FavoriteChangedDTO.class));
    }

    @Test
    void duplicateDeliverySkipped() {
        // 同一 eventId 第二次投递时 SETNX 返回 false，计数只变化一次
        when(valueOperations.setIfAbsent(eq(DEDUP_KEY), eq("1"), any(Duration.class))).thenReturn(false);

        listener.listenFavoriteChanged(event("e1"));

        verify(favoriteCollectService, never()).apply(any());
    }

    @Test
    void failureReleasesDedupKey() {
        when(valueOperations.setIfAbsent(eq(DEDUP_KEY), eq("1"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("db down")).when(favoriteCollectService).apply(any());

        assertThrows(RuntimeException.class, () -> listener.listenFavoriteChanged(event("e1")));

        verify(redisTemplate).delete(DEDUP_KEY);
    }

    @Test
    void eventWithoutEventIdDiscarded() {
        listener.listenFavoriteChanged(new FavoriteChangedDTO());

        verifyNoInteractions(favoriteCollectService);
        verify(redisTemplate, never()).delete(anyString());
    }

    /** 构造一条收藏事件 */
    private FavoriteChangedDTO event(String eventId) {
        FavoriteChangedDTO event = new FavoriteChangedDTO();
        event.setEventId(eventId);
        event.setUid(7L);
        event.setVid(35L);
        event.setCollected(1);
        event.setDelta(1);
        event.setOccurredAt(System.currentTimeMillis());
        return event;
    }
}
