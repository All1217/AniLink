package com.video.web.main.service.impl;

import com.video.model.entity.Favorite;
import com.video.web.main.mapper.FavoriteMapper;
import com.video.web.main.mapper.LegacyUserStatsMapper;
import com.video.web.main.service.FavoriteService;
import com.video.web.main.vo.UserStatsCountsVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Phase 1 过渡适配层：统计聚合与默认收藏夹幂等。 */
@ExtendWith(MockitoExtension.class)
class InternalUserSupportServiceTest {

    @Mock
    private LegacyUserStatsMapper userStatsMapper;
    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private FavoriteService favoriteService;
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

    @Test
    void defaultFavoriteCreatedWhenMissing() {
        when(favoriteMapper.countDefaultFavorite(7L)).thenReturn(0);

        assertTrue(service.ensureDefaultFavorite(7L));

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteService).insertOne(captor.capture());
        assertEquals(7L, captor.getValue().getUid());
        assertEquals(1, captor.getValue().getType());
        assertEquals("默认收藏夹", captor.getValue().getTitle());
    }

    @Test
    void defaultFavoriteSkippedWhenExists() {
        when(favoriteMapper.countDefaultFavorite(7L)).thenReturn(1);

        assertFalse(service.ensureDefaultFavorite(7L));

        verify(favoriteService, never()).insertOne(any());
    }
}
