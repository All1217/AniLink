package com.video.web.admin.vo;

import lombok.Data;

/** 管理端当前登录身份。 */
@Data
public class AdminMeVo {
    private Long uid;
    private String username;
    private String nickname;
    private Integer role;
}
