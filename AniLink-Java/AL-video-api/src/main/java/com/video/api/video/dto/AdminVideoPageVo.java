package com.video.api.video.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端稿件分页结果（内部契约）：只带分页元数据与记录，调用方自行包装成自己的分页结构。
 */
@Data
public class AdminVideoPageVo {
    private List<AdminVideoVo> records;
    private long total;
    private long current;
    private long size;
}
