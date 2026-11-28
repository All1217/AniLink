package com.video.api.video.dto;

import lombok.Data;

/** 跨服务用户视频统计：UP 主名下稿件的获赞/播放/投稿/收藏汇总。 */
@Data
public class UserVideoStatsVo {
    /** 获赞数 */
    private Long likeCount;
    /** 播放数 */
    private Long playCount;
    /** 投稿数 */
    private Integer videoCount;
    /** 收藏数 */
    private Integer collectCount;
}
