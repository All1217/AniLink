package com.video.web.main.service.impl;

import com.video.common.exception.VideoException;
import com.video.model.entity.UserVideo;
import com.video.web.main.mapper.VideoMapper;
import com.video.web.main.mapper.VideoStatsMapper;
import com.video.web.main.service.UserBriefService;
import com.video.web.main.vo.UserVideoQueryVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 投币：actionType 是枚举不是币数，落缓存的值必须是 1/2。 */
@ExtendWith(MockitoExtension.class)
class VideoCoinTest {

    @Mock
    private VideoMapper videoMapper;
    @Mock
    private VideoStatsMapper videoStatsMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private UserBriefService userBriefService;
    @InjectMocks
    private VideoServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void oneCoinStoresOne() {
        UserVideo result = service.interActCoin(query(2));

        assertEquals(1, result.getCoin());
        verify(hashOperations).put("main:video:coin:35", 7L, 1);
    }

    @Test
    void twoCoinsStoreTwo() {
        UserVideo result = service.interActCoin(query(3));

        assertEquals(2, result.getCoin());
        verify(hashOperations).put("main:video:coin:35", 7L, 2);
    }

    @Test
    void legacyValueDoesNotInflateOneCoin() {
        // 旧逻辑把 1 枚写成 2；新逻辑直接写本次币数，不再读旧值
        UserVideo result = service.interActCoin(query(2));

        assertEquals(1, result.getCoin());
        verify(hashOperations).put("main:video:coin:35", 7L, 1);
    }

    @Test
    void invalidActionTypeRejected() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.interActCoin(query(1)));
        assertEquals(202, error.getCode());
    }

    private UserVideoQueryVo query(int actionType) {
        UserVideoQueryVo vo = new UserVideoQueryVo();
        vo.setUid(7L);
        vo.setVid(35L);
        vo.setActionType(actionType);
        return vo;
    }
}
