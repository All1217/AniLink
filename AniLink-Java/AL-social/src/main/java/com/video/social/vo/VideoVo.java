package com.video.social.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.video.model.entity.VideoStats;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 视频信息（Vo）：字段与拆分前 AL-web 的 VideoVo 保持一致，供收藏列表沿用旧响应结构。
 * 数据由 al-video 内部接口补全，不 JOIN video/video_stats。
 */
@Data
@Schema(description = "视频信息（Vo）")
public class VideoVo extends VideoStats {
    @Schema(description = "作者ID")
    private Long uid;

    @Schema(description = "作者昵称")
    private String nickname;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "时长")
    private Double duration;

    @Schema(description = "封面")
    private String coverUrl;

    @Schema(description = "封面缩略图（480w），旧数据回退为 coverUrl")
    private String coverThumbUrl;

    @Schema(description = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date uploadDate;
}
