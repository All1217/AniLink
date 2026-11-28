package com.video.api.video.dto;

import lombok.Data;

import java.time.LocalDate;

/** 跨服务视频日汇总：数据中心按天绘图用，日期按升序返回且缺失日期补 0。 */
@Data
public class VideoDailyStatsVo {
    /** 统计日期 */
    private LocalDate date;
    /** 当日总播放量 */
    private Long play;
    /** 当日粉丝播放量（依赖调用方传入的粉丝 id 集合） */
    private Long fansPlay;
    /** 当日总点赞数 */
    private Long like;
}
