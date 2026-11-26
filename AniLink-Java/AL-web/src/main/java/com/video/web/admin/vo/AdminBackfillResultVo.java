package com.video.web.admin.vo;

import lombok.Data;

/** 「重建封面缩略图」的统计结果。 */
@Data
public class AdminBackfillResultVo {
    private int scanned;
    private int updated;
    private int skipped;
    private int failed;
}
