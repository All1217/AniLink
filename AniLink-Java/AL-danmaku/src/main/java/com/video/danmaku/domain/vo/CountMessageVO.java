package com.video.danmaku.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 服务端 --> 客户端，返回在线人数统计。 */
@Schema(description = "服务端-->客户端，返回在线人数统计")
@Data
public class CountMessageVO {
    @Schema(description = "消息类型")
    private String type;
    @Schema(description = "在线人数")
    private Integer count;
}
