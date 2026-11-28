package com.video.datacenter.service.impl;

import com.video.api.social.SocialInternalClient;
import com.video.api.video.VideoInternalClient;
import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.datacenter.vo.DataResVo;
import com.video.model.entity.HotSearch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 数据中心统计：日期窗口、上一周期口径、粉丝播放量与下游降级。 */
@ExtendWith(MockitoExtension.class)
class DataCenterServiceImplTest {

    @Mock
    private VideoInternalClient videoInternalClient;
    @Mock
    private SocialInternalClient socialInternalClient;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @InjectMocks
    private DataCenterServiceImpl dataCenterService;

    @Test
    @DisplayName("总播放：本周期/上一周期按天切分，粉丝 id 集合透传给 al-video")
    void totalPlaySplitsCurrentAndPreviousPeriod() {
        List<Long> fans = List.of(1L, 2L);
        when(socialInternalClient.fansIds(9L)).thenReturn(fans);
        when(videoInternalClient.dailyStats(any())).thenReturn(List.of(
                row("2025-03-03", 100L, 40L, 10L),
                row("2025-03-04", 200L, 50L, 20L),
                row("2025-03-05", 300L, 60L, 30L),
                row("2025-03-06", 400L, 70L, 40L)
        ));

        DataResVo res = dataCenterService.getTotalPlay(9L, LocalDate.parse("2025-03-05"), LocalDate.parse("2025-03-06"));

        assertEquals(List.of(300L, 400L), res.getMainRes());
        assertEquals(List.of(60L, 70L), res.getSubRes());
        assertEquals(List.of(100L, 200L), res.getLastMainRes());
        assertEquals(List.of(40L, 50L), res.getLastSubRes());
        // 一次 Feign 覆盖上周期起点~本周期终点，粉丝集合随查询透传
        ArgumentCaptor<VideoDailyStatsQueryVo> captor = ArgumentCaptor.forClass(VideoDailyStatsQueryVo.class);
        verify(videoInternalClient).dailyStats(captor.capture());
        assertEquals(9L, captor.getValue().getUid());
        assertEquals(LocalDate.parse("2025-03-03"), captor.getValue().getBegin());
        assertEquals(LocalDate.parse("2025-03-06"), captor.getValue().getEnd());
        assertEquals(fans, captor.getValue().getFansUids());
    }

    @Test
    @DisplayName("总播放：begin>end 自动交换，结果按升序日期切分")
    void totalPlaySwapsReversedDates() {
        when(socialInternalClient.fansIds(9L)).thenReturn(List.of());
        when(videoInternalClient.dailyStats(any())).thenReturn(List.of(
                row("2025-03-05", 1L, 0L, 0L),
                row("2025-03-06", 2L, 0L, 0L)
        ));

        DataResVo res = dataCenterService.getTotalPlay(9L, LocalDate.parse("2025-03-06"), LocalDate.parse("2025-03-05"));

        assertEquals(List.of(1L, 2L), res.getMainRes());
    }

    @Test
    @DisplayName("总播放：al-video 不可用时按 0 补齐序列而不是抛错")
    void totalPlayDegradesToZerosWhenVideoDown() {
        when(socialInternalClient.fansIds(9L)).thenThrow(new RuntimeException("social down"));
        when(videoInternalClient.dailyStats(any())).thenThrow(new RuntimeException("video down"));

        DataResVo res = dataCenterService.getTotalPlay(9L, LocalDate.parse("2025-03-05"), LocalDate.parse("2025-03-06"));

        assertEquals(List.of(0L, 0L), res.getMainRes());
        assertEquals(List.of(0L, 0L), res.getSubRes());
        assertEquals(List.of(0L, 0L), res.getLastMainRes());
        assertEquals(List.of(0L, 0L), res.getLastSubRes());
    }

    @Test
    @DisplayName("总点赞：只返回本期/上一周期主序列，不填粉丝维度")
    void likeOnlyFillsMainSeries() {
        when(videoInternalClient.dailyStats(any())).thenReturn(List.of(
                row("2025-03-04", 0L, 0L, 20L),
                row("2025-03-05", 0L, 0L, 30L)
        ));

        DataResVo res = dataCenterService.getLike(9L, LocalDate.parse("2025-03-05"), LocalDate.parse("2025-03-05"));

        assertEquals(List.of(30L), res.getMainRes());
        assertEquals(List.of(20L), res.getLastMainRes());
        assertNull(res.getSubRes());
        assertNull(res.getLastSubRes());
        // 点赞不需要粉丝数据，不额外调用社交域
        verify(socialInternalClient, never()).fansIds(any());
    }

    @Test
    @DisplayName("热搜：读取引擎写入 Redis 的 hotSearch#list")
    void hotSearchReadsEngineCache() {
        HotSearch hot = new HotSearch();
        hot.setKeyword("测试热搜");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hotSearch")).thenReturn(Map.of("list", List.of(hot)));

        List<HotSearch> result = dataCenterService.getHotSearch();

        assertEquals(1, result.size());
        assertEquals("测试热搜", result.get(0).getKeyword());
    }

    @Test
    @DisplayName("热搜：缓存为空或读取异常时返回空列表")
    void hotSearchDegradesToEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("hotSearch")).thenReturn(null);
        assertTrue(dataCenterService.getHotSearch().isEmpty());

        when(valueOperations.get("hotSearch")).thenThrow(new RuntimeException("redis down"));
        assertTrue(dataCenterService.getHotSearch().isEmpty());
    }

    /** 构造 al-video 返回的逐日汇总行 */
    private VideoDailyStatsVo row(String date, Long play, Long fansPlay, Long like) {
        VideoDailyStatsVo vo = new VideoDailyStatsVo();
        vo.setDate(LocalDate.parse(date));
        vo.setPlay(play);
        vo.setFansPlay(fansPlay);
        vo.setLike(like);
        return vo;
    }
}
