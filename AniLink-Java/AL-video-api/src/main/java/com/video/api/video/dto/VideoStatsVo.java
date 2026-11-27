package com.video.api.video.dto;

import lombok.Data;

/** 跨服务视频统计：点赞/投币/收藏等计数。 */
@Data
public class VideoStatsVo {
    private Long vid;
    private Long play;
    private Long danmu;
    private Long good;
    private Long bad;
    private Long coin;
    private Long collect;
    private Long share;
    private Long comment;
}
