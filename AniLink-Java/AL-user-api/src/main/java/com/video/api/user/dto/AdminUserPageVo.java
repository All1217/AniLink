package com.video.api.user.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理端用户分页结果（内部契约）：只带分页元数据与记录，调用方自行包装成自己的分页结构。
 */
@Data
public class AdminUserPageVo {
    private List<AdminUserVo> records;
    private long total;
    private long current;
    private long size;
}
