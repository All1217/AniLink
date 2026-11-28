package com.video.api.user.dto;

import lombok.Data;

/**
 * 管理员重置密码请求（内部契约）：传临时密码明文，由 al-user 负责 bcrypt 哈希与 password_updated_at。
 * 密码走请求体而不是 URL，避免出现在访问日志/链路参数里。
 */
@Data
public class AdminPasswordResetVo {
    private String password;
}
