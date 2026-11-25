package com.video.web.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.common.login.LoginUserHolder;
import com.video.common.result.Result;
import com.video.web.admin.service.AdminUserService;
import com.video.web.admin.vo.AdminUserQueryVo;
import com.video.web.admin.vo.AdminUserRoleVo;
import com.video.web.admin.vo.AdminUserStatusVo;
import com.video.web.admin.vo.AdminUserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端用户接口。 */
@Tag(name = "管理端-用户")
@RestController
@RequestMapping("/main/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户分页")
    @GetMapping
    public Result<IPage<AdminUserVo>> page(AdminUserQueryVo query) {
        return Result.ok(adminUserService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{uid}")
    public Result<AdminUserVo> detail(@PathVariable Long uid) {
        return Result.ok(adminUserService.detail(uid));
    }

    @Operation(summary = "封禁/解封用户")
    @PutMapping("/{uid}/status")
    public Result<Void> updateStatus(@PathVariable Long uid, @RequestBody AdminUserStatusVo body) {
        adminUserService.updateStatus(operatorUid(), uid, body.getStatus());
        return Result.ok();
    }

    @Operation(summary = "调整用户角色（仅超级管理员）")
    @PutMapping("/{uid}/role")
    public Result<Void> updateRole(@PathVariable Long uid, @RequestBody AdminUserRoleVo body) {
        adminUserService.updateRole(operatorUid(), uid, body.getRole());
        return Result.ok();
    }

    private Long operatorUid() {
        return LoginUserHolder.getLoginUser().getUserId();
    }
}
