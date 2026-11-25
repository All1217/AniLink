package com.video.web.admin.vo;

import lombok.Data;

/** 管理端稿件分页查询条件。 */
@Data
public class AdminVideoQueryVo {
    private long current = 1;
    private long size = 10;
    private Integer status;
    private Long uid;
    private String keyword;
}
