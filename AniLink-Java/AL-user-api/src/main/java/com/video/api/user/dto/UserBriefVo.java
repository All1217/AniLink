package com.video.api.user.dto;

import lombok.Data;

/** 跨服务用户摘要：列表/评论/聊天补全作者信息用，不包含密码等敏感字段。 */
@Data
public class UserBriefVo {
    private Long uid;
    private String nickname;
    private String avatar;
    /** 个性签名，粉丝/关注列表展示用 */
    private String description;
    /** 账号状态，粉丝/关注列表展示用 */
    private Integer status;
}
