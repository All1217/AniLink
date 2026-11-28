package com.video.datacenter.service.impl;

import com.video.api.user.dto.UserBriefVo;
import com.video.datacenter.adapter.RecommendEngineClient;
import com.video.datacenter.service.UserBriefService;
import com.video.datacenter.service.VideoBriefService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.VideoVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 实时推荐：引擎给 id，视频/作者信息走内部契约批量补全；引擎空结果不触发后续查询。 */
@ExtendWith(MockitoExtension.class)
class RecommendServiceImplTest {

    @Mock
    private RecommendEngineClient engineClient;
    @Mock
    private VideoBriefService videoBriefService;
    @Mock
    private UserBriefService userBriefService;
    @InjectMocks
    private RecommendServiceImpl recommendService;

    @Test
    @DisplayName("推荐：按引擎 id 批量补全视频并填作者昵称")
    void recommendFillsVideosAndNicknames() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setUid(7L);
        queryVo.setCount(10);
        when(engineClient.recommendVideoIds(7L, 10)).thenReturn(List.of(3L, 1L));
        VideoVo first = video(3L, 7L);
        VideoVo second = video(1L, 8L);
        when(videoBriefService.legacyVideosInPlayOrder(List.of(3L, 1L))).thenReturn(List.of(first, second));
        when(userBriefService.byUids(List.of(7L, 8L)))
                .thenReturn(Map.of(7L, user(7L, "作者A"), 8L, user(8L, "作者B")));

        List<VideoVo> result = recommendService.getRealTimeRecommend(queryVo);

        assertEquals(2, result.size());
        assertEquals("作者A", first.getNickname());
        assertEquals("作者B", second.getNickname());
    }

    @Test
    @DisplayName("推荐：引擎返回空列表时直接返回空，不查视频/用户")
    void recommendEmptyWhenEngineReturnsNothing() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setUid(7L);
        queryVo.setCount(10);
        when(engineClient.recommendVideoIds(7L, 10)).thenReturn(List.of());

        assertTrue(recommendService.getRealTimeRecommend(queryVo).isEmpty());
        verifyNoInteractions(videoBriefService, userBriefService);
    }

    @Test
    @DisplayName("推荐：不存在的用户保留空昵称，不阻断列表")
    void recommendKeepsNullNicknameForMissingUser() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setUid(7L);
        when(engineClient.recommendVideoIds(7L, null)).thenReturn(List.of(3L));
        VideoVo video = video(3L, 7L);
        when(videoBriefService.legacyVideosInPlayOrder(List.of(3L))).thenReturn(List.of(video));
        when(userBriefService.byUids(any())).thenReturn(Map.of());

        List<VideoVo> result = recommendService.getRealTimeRecommend(queryVo);

        assertEquals(1, result.size());
        assertNull(result.get(0).getNickname());
    }

    /** 构造推荐视频 */
    private VideoVo video(Long vid, Long uid) {
        VideoVo vo = new VideoVo();
        vo.setVid(vid);
        vo.setUid(uid);
        return vo;
    }

    /** 构造用户摘要 */
    private UserBriefVo user(Long uid, String nickname) {
        UserBriefVo vo = new UserBriefVo();
        vo.setUid(uid);
        vo.setNickname(nickname);
        return vo;
    }
}
