package com.video.datacenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 数据分析结果 Vo：本周期的总量/粉丝量序列 + 上一周期的同口径序列，字段与拆分前保持一致。 */
@Data
@Schema(description = "数据分析结果Vo")
public class DataResVo {
    @Schema(description = "结果集1")
    private List<Long> mainRes;
    @Schema(description = "结果集2")
    private List<Long> subRes;
    @Schema(description = "上周期结果集1")
    private List<Long> lastMainRes;
    @Schema(description = "上周期结果集2")
    private List<Long> lastSubRes;
}
