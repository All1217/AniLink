package com.video.api.user.dto;

import lombok.Data;

/**
 * 用户侧存储对象引用（内部契约）：对象存储删除前查该 URL 是否仍被用户头像/背景引用。
 */
@Data
public class AdminUserRefVo {
    /** 引用方类型，固定 user */
    private String type;
    /** 用户 uid */
    private Long id;
    /** 用户昵称 */
    private String name;
    /** 引用字段：avatar / background */
    private String field;
}
