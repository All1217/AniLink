package com.video.api.video.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 视频日汇总查询：按 UP 主与时间范围统计每日播放/点赞。
 * fansUids 由调用方（数据中心 BFF）从社交域取粉丝 id 后传入，视频域不反向查询 follow 表。
 */
@Data
public class VideoDailyStatsQueryVo {
    /** UP 主 uid */
    private Long uid;
    /** 起始日期（含） */
    private LocalDate begin;
    /** 结束日期（含） */
    private LocalDate end;
    /** UP 主粉丝 id 集合，用于拆出粉丝播放量；为空时粉丝播放量按 0 返回 */
    private List<Long> fansUids;
}
