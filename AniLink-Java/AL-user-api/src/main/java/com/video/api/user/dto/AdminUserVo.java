package com.video.api.user.dto;

import lombok.Data;

import java.util.Date;

/**
 * 管理端用户视图（内部契约）：只暴露管理页需要的字段，不含密码等敏感信息。
 * 字段与 AL-admin 对外返回的 AdminUserVo 保持一致，避免外层再做二次拼装。
 */
@Data
public class AdminUserVo {
    private Long uid;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private Integer role;
    /** 创建时间；跨服务按 Jackson 默认日期格式传输，对外输出格式由调用方 VO 决定 */
    private Date createDate;
}
