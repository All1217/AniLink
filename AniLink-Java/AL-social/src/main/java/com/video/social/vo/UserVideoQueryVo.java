package com.video.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户与视频交互查询入参（收藏状态查询用）。
 * 收藏状态以 favorite/favorite_video 为唯一事实来源，不再读取 al-video 的 user_video。
 */
@Data
@Schema(description = "用户与视频交互Vo")
public class UserVideoQueryVo {
    @Schema(description = "用户唯一标识")
    private Long uid;

    @Schema(description = "视频标识")
    private Long vid;

    @Schema(description = "交互类型")
    private Integer actionType;
}
