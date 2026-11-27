package com.video.web.main.service.impl;

import com.video.web.main.mapper.LegacyUserStatsMapper;
import com.video.web.main.vo.UserStatsCountsVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Phase 1 过渡适配层：统计聚合（默认收藏夹创建已迁到 al-social）。 */
@ExtendWith(MockitoExtension.class)
class InternalUserSupportServiceTest {

    @Mock
    private LegacyUserStatsMapper userStatsMapper;
    @InjectMocks
    private InternalUserSupportService service;

    @Test
    void statsAggregationMapsAllCounts() {
        when(userStatsMapper.calFansCount(7L)).thenReturn(11L);
        when(userStatsMapper.calFollowCount(7L)).thenReturn(3);
        when(userStatsMapper.calLikeCount(7L)).thenReturn(22L);
        when(userStatsMapper.calPlayCount(7L)).thenReturn(33L);
        when(userStatsMapper.calVideoCount(7L)).thenReturn(4);
        when(userStatsMapper.calCollectCount(7L)).thenReturn(5);

        UserStatsCountsVo vo = service.statsOf(7L);

        assertEquals(11L, vo.getFansCount());
        assertEquals(3, vo.getFollowCount());
        assertEquals(22L, vo.getLikeCount());
        assertEquals(33L, vo.getPlayCount());
        assertEquals(4, vo.getVideoCount());
        assertEquals(5, vo.getCollectCount());
    }
}
