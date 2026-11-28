package com.video.datacenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 推荐/标签接口入参：字段与拆分前的 RecQueryVo 保持一致（vid 在标签接口里表示另一个用户 id）。 */
@Data
@Schema(description = "推荐服务Vo")
public class RecQueryVo {
    @Schema(description = "用户ID")
    private Long uid;

    @Schema(description = "推荐视频数量")
    private Integer count;

    @Schema(description = "视频ID/用户ID")
    private Long vid;

    @Schema(description = "指定的用户标签")
    private Integer tag;

    @Schema(description = "指定的用户标签（字符串）")
    private String tagName;
}
