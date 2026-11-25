package com.video.web.admin.controller;

import com.video.common.login.LoginUserHolder;
import com.video.common.result.Result;
import com.video.model.entity.UserInfo;
import com.video.web.admin.vo.AdminMeVo;
import com.video.web.main.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端身份确认。 */
@Tag(name = "管理端-身份")
@RestController
@RequestMapping("/main/admin")
public class AdminAuthController {

    private final UserInfoService userInfoService;

    public AdminAuthController(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @Operation(summary = "当前管理员身份")
    @GetMapping("/me")
    public Result<AdminMeVo> me() {
        Long uid = LoginUserHolder.getLoginUser().getUserId();
        UserInfo user = userInfoService.getById(uid);
        AdminMeVo vo = new AdminMeVo();
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return Result.ok(vo);
    }
}
