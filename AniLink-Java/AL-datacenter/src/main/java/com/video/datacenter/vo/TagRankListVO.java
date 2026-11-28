package com.video.datacenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 出现频率前八的标签：字段名 name/value 与前端 ECharts 排行一致。 */
@Data
@Schema(description = "出现频率排行前八的标签VO")
public class TagRankListVO {
    @Schema(description = "标签名")
    private String name;

    @Schema(description = "出现次数")
    private Integer value;
}
