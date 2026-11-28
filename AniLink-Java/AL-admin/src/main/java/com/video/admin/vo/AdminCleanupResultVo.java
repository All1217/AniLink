package com.video.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 一键清理冗余对象的结果统计。 */
@Data
public class AdminCleanupResultVo {
    private int scanned;
    private int deleted;
    private int skipped;
    private int failed;
    private List<String> failedKeys = new ArrayList<>();
}
