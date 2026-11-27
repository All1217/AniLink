package com.video.playback.vo;

import com.video.model.entity.BrowseHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "历史记录信息")
public class HistoryVo extends BrowseHistory {
    @Schema(description = "作者UID")
    private Long authorUid;

    @Schema(description = "作者昵称")
    private String nickname;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "视频时长")
    private Double videoDuration;

    @Schema(description = "封面")
    private String coverUrl;

    @Schema(description = "封面缩略图（480w），旧数据回退为 coverUrl")
    private String coverThumbUrl;
}
