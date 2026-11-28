package com.video.video.service.impl;

import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.video.mapper.UserVideoMapper;
import com.video.video.service.VideoDailyStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频日汇总实现：只聚合属主表 video 与 user_video。
 * 粉丝播放量依赖数据中心传入的粉丝 id 集合，本服务不反向查询社交域的 follow 表。
 */
@Slf4j
@Service
public class VideoDailyStatsServiceImpl implements VideoDailyStatsService {

    /** 单次查询最大天数，防止异常入参生成超长响应 */
    private static final int MAX_DAYS = 366;

    private final UserVideoMapper userVideoMapper;

    public VideoDailyStatsServiceImpl(UserVideoMapper userVideoMapper) {
        this.userVideoMapper = userVideoMapper;
    }

    @Override
    public List<VideoDailyStatsVo> dailyStats(VideoDailyStatsQueryVo query) {
        // 1.参数校验与日期归一
        if (query == null || query.getUid() == null || query.getBegin() == null || query.getEnd() == null) {
            return List.of();
        }
        LocalDate begin = query.getBegin();
        LocalDate end = query.getEnd();
        if (begin.isAfter(end)) {
            LocalDate temp = begin;
            begin = end;
            end = temp;
        }
        if (begin.plusDays(MAX_DAYS).isBefore(end)) {
            log.warn("日汇总查询范围过大，已截断：uid={}, begin={}, end={}", query.getUid(), begin, end);
            end = begin.plusDays(MAX_DAYS - 1L);
        }
        // 2.一次查询覆盖整个范围，按天分组
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();
        List<VideoDailyStatsVo> rows = userVideoMapper.dailyStats(query, beginTime, endTime);
        Map<LocalDate, VideoDailyStatsVo> byDate = new HashMap<>();
        if (rows != null) {
            rows.stream().filter(row -> row.getDate() != null).forEach(row -> byDate.put(row.getDate(), row));
        }
        // 3.按日期升序补 0，前端按等长序列绘图
        List<VideoDailyStatsVo> result = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            VideoDailyStatsVo row = byDate.get(date);
            if (row == null) {
                row = new VideoDailyStatsVo();
                row.setDate(date);
                row.setPlay(0L);
                row.setFansPlay(0L);
                row.setLike(0L);
            }
            result.add(row);
        }
        return result;
    }
}
