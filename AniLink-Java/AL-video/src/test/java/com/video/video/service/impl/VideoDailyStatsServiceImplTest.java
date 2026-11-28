package com.video.video.service.impl;

import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.video.mapper.UserVideoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 视频日汇总：日期升序补 0、范围归一与入参校验。 */
@ExtendWith(MockitoExtension.class)
class VideoDailyStatsServiceImplTest {

    @Mock
    private UserVideoMapper userVideoMapper;
    @InjectMocks
    private VideoDailyStatsServiceImpl service;

    @Test
    @DisplayName("缺失日期补 0，查询时间范围为 [begin 00:00, end+1 00:00)")
    void dailyStatsFillsMissingDays() {
        when(userVideoMapper.dailyStats(any(), any(), any())).thenReturn(List.of(
                row("2025-03-06", 5L, 2L, 3L)));

        List<VideoDailyStatsVo> result = service.dailyStats(query("2025-03-05", "2025-03-06"));

        assertEquals(2, result.size());
        assertEquals(LocalDate.parse("2025-03-05"), result.get(0).getDate());
        assertEquals(0L, result.get(0).getPlay());
        assertEquals(LocalDate.parse("2025-03-06"), result.get(1).getDate());
        assertEquals(5L, result.get(1).getPlay());
        assertEquals(2L, result.get(1).getFansPlay());
        assertEquals(3L, result.get(1).getLike());
        ArgumentCaptor<LocalDateTime> beginCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userVideoMapper).dailyStats(any(), beginCaptor.capture(), endCaptor.capture());
        assertEquals(LocalDateTime.parse("2025-03-05T00:00"), beginCaptor.getValue());
        assertEquals(LocalDateTime.parse("2025-03-07T00:00"), endCaptor.getValue());
    }

    @Test
    @DisplayName("begin>end 时交换日期，序列仍按升序")
    void dailyStatsSwapsReversedRange() {
        when(userVideoMapper.dailyStats(any(), any(), any())).thenReturn(List.of());

        List<VideoDailyStatsVo> result = service.dailyStats(query("2025-03-06", "2025-03-05"));

        assertEquals(2, result.size());
        assertEquals(LocalDate.parse("2025-03-05"), result.get(0).getDate());
        assertEquals(LocalDate.parse("2025-03-06"), result.get(1).getDate());
    }

    @Test
    @DisplayName("入参不完整时返回空列表且不查库")
    void dailyStatsRejectsIncompleteQuery() {
        assertTrue(service.dailyStats(null).isEmpty());
        assertTrue(service.dailyStats(new VideoDailyStatsQueryVo()).isEmpty());
        verify(userVideoMapper, never()).dailyStats(any(), any(), any());
    }

    /** 构造日汇总查询 */
    private VideoDailyStatsQueryVo query(String begin, String end) {
        VideoDailyStatsQueryVo query = new VideoDailyStatsQueryVo();
        query.setUid(7L);
        query.setBegin(LocalDate.parse(begin));
        query.setEnd(LocalDate.parse(end));
        query.setFansUids(List.of(1L, 2L));
        return query;
    }

    /** 构造数据库返回的日汇总行 */
    private VideoDailyStatsVo row(String date, Long play, Long fansPlay, Long like) {
        VideoDailyStatsVo vo = new VideoDailyStatsVo();
        vo.setDate(LocalDate.parse(date));
        vo.setPlay(play);
        vo.setFansPlay(fansPlay);
        vo.setLike(like);
        return vo;
    }
}
