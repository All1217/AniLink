package com.video.social.service.impl;

import com.video.model.entity.FavoriteVideo;
import com.video.model.entity.UserVideo;
import com.video.social.mapper.FavoriteMapper;
import com.video.social.mq.FavoriteEventPublisher;
import com.video.social.service.UserBriefService;
import com.video.social.service.VideoBriefService;
import com.video.social.vo.BatchCollectVo;
import com.video.social.vo.BatchUpdateVo;
import com.video.social.vo.UserVideoQueryVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 收藏域：收藏状态以属主表为准，状态翻转才发事件，收藏夹之间移动不发事件。 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteMapper favoriteMapper;
    @Mock
    private UserBriefService userBriefService;
    @Mock
    private VideoBriefService videoBriefService;
    @Mock
    private FavoriteEventPublisher eventPublisher;
    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    @Test
    void collectFirstTimePublishesIncreaseEvent() {
        when(favoriteMapper.getFVByUidAndVid(7L, 35L)).thenReturn(List.of());

        favoriteService.collect(collectVo(7L, 35L, List.of(1L)));

        verify(favoriteMapper).batchInsertSv(argThat(fv -> isFavoriteVideo(fv, 1L, 35L)));
        verify(favoriteMapper).addCount(1, 1L);
        verify(eventPublisher).publishAfterCommit(7L, 35L, 1, 1);
    }

    @Test
    void collectMoveBetweenFavoritesDoesNotPublish() {
        when(favoriteMapper.getFVByUidAndVid(7L, 35L)).thenReturn(List.of(1L));

        favoriteService.collect(collectVo(7L, 35L, List.of(2L)));

        verify(favoriteMapper).deleteFVById(1L, 35L);
        verify(favoriteMapper).subCount(1, 1L);
        verify(favoriteMapper).batchInsertSv(argThat(fv -> isFavoriteVideo(fv, 2L, 35L)));
        verify(favoriteMapper).addCount(1, 2L);
        verify(eventPublisher, never()).publishAfterCommit(7L, 35L, 1, 1);
        verify(eventPublisher, never()).publishAfterCommit(7L, 35L, 0, -1);
    }

    @Test
    void collectRemoveAllPublishesDecreaseEvent() {
        when(favoriteMapper.getFVByUidAndVid(7L, 35L)).thenReturn(List.of(1L, 2L));

        favoriteService.collect(collectVo(7L, 35L, List.of()));

        verify(favoriteMapper).deleteFVById(1L, 35L);
        verify(favoriteMapper).deleteFVById(2L, 35L);
        verify(eventPublisher).publishAfterCommit(7L, 35L, 0, -1);
        verify(eventPublisher, never()).publishAfterCommit(7L, 35L, 1, 1);
    }

    @Test
    void unCollectLastFavoritesPublishesDecreaseEvent() {
        when(favoriteMapper.getFavoriteOwner(1L)).thenReturn(7L);
        when(favoriteMapper.countCollected(7L, 35L)).thenReturn(0);

        favoriteService.unCollect(updateVo(1L, 35L));

        verify(favoriteMapper).subCount(1, 1L);
        verify(favoriteMapper).deleteFVById(1L, 35L);
        verify(eventPublisher).publishAfterCommit(7L, 35L, 0, -1);
    }

    @Test
    void unCollectStillInOtherFavoritesSkipsEvent() {
        when(favoriteMapper.getFavoriteOwner(1L)).thenReturn(7L);
        when(favoriteMapper.countCollected(7L, 35L)).thenReturn(1);

        favoriteService.unCollect(updateVo(1L, 35L));

        verify(eventPublisher, never()).publishAfterCommit(7L, 35L, 0, -1);
    }

    @Test
    void interactionFavoriteReadsOwnedTablesOnly() {
        when(favoriteMapper.countCollected(7L, 35L)).thenReturn(2);

        UserVideo result = favoriteService.getInterActionFavorite(interactionQuery(7L, 35L));

        assertEquals(1, result.getCollect());
        verify(favoriteMapper).countCollected(7L, 35L);
    }

    /** 构造「收藏到一个或多个收藏夹」入参 */
    private BatchCollectVo collectVo(Long uid, Long vid, List<Long> fids) {
        BatchCollectVo vo = new BatchCollectVo();
        vo.setUid(uid);
        vo.setVid(vid);
        vo.setFids(fids);
        return vo;
    }

    /** 构造「从收藏夹移除视频」入参 */
    private BatchUpdateVo updateVo(Long fid, Long vid) {
        BatchUpdateVo vo = new BatchUpdateVo();
        vo.setPreFid(fid);
        vo.setVids(List.of(vid));
        return vo;
    }

    /** 构造收藏状态查询入参 */
    private UserVideoQueryVo interactionQuery(Long uid, Long vid) {
        UserVideoQueryVo vo = new UserVideoQueryVo();
        vo.setUid(uid);
        vo.setVid(vid);
        return vo;
    }

    /** 收藏关系记录是否符合预期 */
    private boolean isFavoriteVideo(FavoriteVideo fv, Long fid, Long vid) {
        return fv != null && fid.equals(fv.getFid()) && vid.equals(fv.getVid())
                && Integer.valueOf(0).equals(fv.getIsRemove()) && fv.getTime() != null;
    }
}
