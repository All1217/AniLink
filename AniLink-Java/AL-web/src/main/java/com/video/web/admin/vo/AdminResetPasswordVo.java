package com.video.web.admin.vo;

import lombok.Data;

/** 管理员重置密码后返回的临时密码，仅返回一次。 */
@Data
public class AdminResetPasswordVo {
    private Long uid;
    private String username;
    private String tempPassword;
}
