package com.video.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码")
public class ChangePasswordVo {
    @Schema(description = "当前密码")
    private String oldPassword;

    @Schema(description = "新密码（至少 8 位，最多 72 字节）")
    private String newPassword;
}
