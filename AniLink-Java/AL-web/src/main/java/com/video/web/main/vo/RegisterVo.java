package com.video.web.main.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "注册信息")
public class RegisterVo {
    @Schema(description = "登录名（用户名），唯一，登录时使用")
    private String username;

    @Schema(description = "密码（至少 8 位，最多 72 字节）")
    private String password;

    @Schema(description = "昵称，可重复；不填则默认与用户名相同")
    private String nickname;
}
