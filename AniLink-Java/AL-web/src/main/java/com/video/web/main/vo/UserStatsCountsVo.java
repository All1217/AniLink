package com.video.web.main.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户统计聚合结果（迁移过渡用）。
 * 这些计数来自 follow / video / video_stats，Phase 1 仍由 AL-web 计算，
 * 通过 /internal/v1 暴露给 al-user，Phase 3/4 各域迁走后删除。
 */
@Data
@Schema(description = "用户统计聚合（过渡接口）")
public class UserStatsCountsVo {
    @Schema(description = "粉丝数")
    private Long fansCount;

    @Schema(description = "关注数")
    private Integer followCount;

    @Schema(description = "获赞数")
    private Long likeCount;

    @Schema(description = "播放数")
    private Long playCount;

    @Schema(description = "投稿数")
    private Integer videoCount;

    @Schema(description = "收藏数")
    private Integer collectCount;
}
