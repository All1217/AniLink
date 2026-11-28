package com.video.admin.controller;

import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserProfileVo;
import com.video.common.exception.VideoException;
import com.video.common.login.LoginUserHolder;
import com.video.common.result.Result;
import com.video.common.result.ResultCodeEnum;
import com.video.admin.vo.AdminMeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端身份确认：用户资料已归 al-user，经内部接口读取。 */
@Tag(name = "管理端-身份")
@RestController
@RequestMapping("/main/admin")
public class AdminAuthController {

    private final ObjectProvider<UserInternalClient> clientProvider;

    public AdminAuthController(ObjectProvider<UserInternalClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    @Operation(summary = "当前管理员身份")
    @GetMapping("/me")
    public Result<AdminMeVo> me() {
        Long uid = LoginUserHolder.getLoginUser().getUserId();
        UserProfileVo profile = clientProvider.getObject().profile(uid);
        if (profile == null) {
            throw new VideoException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        AdminMeVo vo = new AdminMeVo();
        vo.setUid(profile.getUid());
        vo.setUsername(profile.getUsername());
        vo.setNickname(profile.getNickname());
        vo.setRole(profile.getRole());
        return Result.ok(vo);
    }
}
