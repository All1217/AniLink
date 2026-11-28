package com.video.api.social.dto;

import lombok.Data;

/** 跨服务用户关注统计：粉丝数与关注数，都由 follow 表的两个方向计数得到。 */
@Data
public class UserFollowStatsVo {
    /** 粉丝数（followee_id = uid） */
    private Long fansCount;
    /** 关注数（follower_id = uid） */
    private Integer followCount;
}
