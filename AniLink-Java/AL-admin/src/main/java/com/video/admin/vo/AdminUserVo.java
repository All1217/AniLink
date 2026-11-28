package com.video.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/** 管理端用户列表/详情，不包含密码。 */
@Data
public class AdminUserVo {
    private Long uid;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private Integer role;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createDate;
}
