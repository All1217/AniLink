package com.video.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录/注册结果：access-token 与用户基本信息（refresh-token 在 HttpOnly Cookie 里）")
public class LoginResultVo {
    @Schema(description = "访问令牌，放在请求头 access-token")
    private String accessToken;

    @Schema(description = "用户ID")
    private Long uid;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;
}
