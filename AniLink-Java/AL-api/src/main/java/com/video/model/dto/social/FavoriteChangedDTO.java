package com.video.model.dto.social;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收藏状态变更事件（al-social → al-video）。
 *
 * <p>交换机 {@code social.favorite.topic}，路由键 {@code favorite.changed}。
 * 消费方以 {@code eventId} 去重，按 {@code delta} 增量更新视频收藏计数，
 * 按 {@code collected} 覆盖用户×视频的收藏标记。</p>
 */
@Schema(description = "收藏状态变更事件")
@Data
public class FavoriteChangedDTO {

    /** 事件唯一标识，消费方幂等去重用 */
    @Schema(description = "事件唯一标识")
    private String eventId;

    /** 收藏操作的用户 ID */
    @Schema(description = "用户ID")
    private Long uid;

    /** 收藏操作的视频 ID */
    @Schema(description = "视频ID")
    private Long vid;

    /** 变更后的收藏状态：1 已收藏，0 未收藏 */
    @Schema(description = "变更后的收藏状态：1 已收藏，0 未收藏")
    private Integer collected;

    /** 视频收藏计数增量：收藏 +1，取消收藏 -1 */
    @Schema(description = "收藏计数增量")
    private Integer delta;

    /** 事件发生时间（毫秒时间戳），便于排查与对账 */
    @Schema(description = "事件发生时间（毫秒时间戳）")
    private Long occurredAt;
}
