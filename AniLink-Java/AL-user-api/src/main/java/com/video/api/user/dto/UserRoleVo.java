package com.video.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 账号角色：0 普通用户 / 1 管理员 / 2 超级管理员，管理端鉴权用。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleVo {
    private Long uid;
    private Integer role;
}
