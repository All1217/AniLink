package com.video.api.user.dto;

import lombok.Data;

/** 跨服务用户统计：粉丝/关注/获赞/播放/投稿/收藏。 */
@Data
public class UserStatsVo {
    private Long uid;
    private Long fansCount;
    private Integer followCount;
    private Long likeCount;
    private Long playCount;
    private Integer videoCount;
    private Integer collectCount;
}
