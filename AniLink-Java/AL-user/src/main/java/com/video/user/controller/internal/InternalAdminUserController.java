package com.video.user.controller.internal;

import com.video.api.user.dto.AdminPasswordResetVo;
import com.video.api.user.dto.AdminUserPageVo;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.api.user.dto.AdminUserVo;
import com.video.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户域管理契约实现，只给 AL-admin 调用（网关不暴露 /internal/**）。
 * 返回裸 DTO 而不是 Result，便于 Feign 直接反序列化。
 */
@Tag(name = "内部-管理用户")
@RestController
@RequestMapping("/internal/v1/admin/users")
public class InternalAdminUserController {

    private final AdminUserService adminUserService;

    public InternalAdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "管理端用户分页")
    @GetMapping
    public AdminUserPageVo page(@RequestParam(value = "current", defaultValue = "1") long current,
                                @RequestParam(value = "size", defaultValue = "10") long size,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "status", required = false) Integer status,
                                @RequestParam(value = "role", required = false) Integer role) {
        return adminUserService.page(current, size, keyword, status, role);
    }

    @Operation(summary = "管理端用户详情（不存在返回 null）")
    @GetMapping("/{uid}")
    public AdminUserVo detail(@PathVariable("uid") Long uid) {
        return adminUserService.detail(uid);
    }

    @Operation(summary = "按 uid 列表批量取用户")
    @PostMapping("/batch")
    public List<AdminUserVo> batch(@RequestBody List<Long> uids) {
        return adminUserService.listByUids(uids);
    }

    @Operation(summary = "更新账号状态")
    @PutMapping("/{uid}/status")
    public void updateStatus(@PathVariable("uid") Long uid, @RequestParam("status") Integer status) {
        adminUserService.updateStatus(uid, status);
    }

    @Operation(summary = "更新账号角色")
    @PutMapping("/{uid}/role")
    public void updateRole(@PathVariable("uid") Long uid, @RequestParam("role") Integer role) {
        adminUserService.updateRole(uid, role);
    }

    @Operation(summary = "重置密码（写 password + password_updated_at）")
    @PostMapping("/{uid}/password/reset")
    public int resetPassword(@PathVariable("uid") Long uid, @RequestBody AdminPasswordResetVo body) {
        return adminUserService.resetPassword(uid, body == null ? null : body.getPassword());
    }

    @Operation(summary = "按完整 URL 查用户头像/背景引用")
    @GetMapping("/refs")
    public List<AdminUserRefVo> refs(@RequestParam("url") String url) {
        return adminUserService.refsByUrl(url);
    }
}
