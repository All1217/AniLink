package com.video.api.user.dto;

import lombok.Data;

/** 管理端身份资料：uid / 登录名 / 昵称 / 角色，仅内部接口返回。 */
@Data
public class UserProfileVo {
    private Long uid;
    private String username;
    private String nickname;
    private Integer role;
}
