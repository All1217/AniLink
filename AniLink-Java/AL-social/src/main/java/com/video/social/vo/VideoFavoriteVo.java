package com.video.social.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 收藏视频列表项：视频信息 + 收藏时间，字段与拆分前保持一致。 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "视频信息（Vo）")
public class VideoFavoriteVo extends VideoVo {
    @Schema(description = "收藏时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;
}
