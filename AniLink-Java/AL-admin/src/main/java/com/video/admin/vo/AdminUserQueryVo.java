package com.video.admin.vo;

import lombok.Data;

/** 管理端用户分页查询条件。 */
@Data
public class AdminUserQueryVo {
    private long current = 1;
    private long size = 10;
    private String keyword;
    private Integer status;
    private Integer role;
}
