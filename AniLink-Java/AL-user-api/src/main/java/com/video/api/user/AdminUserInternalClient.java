package com.video.api.user;

import com.video.api.user.dto.AdminPasswordResetVo;
import com.video.api.user.dto.AdminUserPageVo;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.api.user.dto.AdminUserVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户域管理契约：只给 AL-admin 调用，网关不暴露 /internal/**。
 * 用户表归 al-user 所有，管理端的查询与改状态/角色/密码都经这里，禁止跨库直连。
 * 角色校验等管理端业务规则留在调用方，这里只做数据读写。
 */
@FeignClient(name = "userservice", contextId = "adminUserInternalClient",
        url = "${al.user.service-url:}", path = "/internal/v1/admin/users")
public interface AdminUserInternalClient {

    /** 用户分页：关键字匹配 username/nickname/uid，status、role 为可选的精确过滤 */
    @GetMapping
    AdminUserPageVo page(@RequestParam("current") long current,
                         @RequestParam("size") long size,
                         @RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "status", required = false) Integer status,
                         @RequestParam(value = "role", required = false) Integer role);

    /** 用户详情（不含密码）；不存在返回 null */
    @GetMapping("/{uid}")
    AdminUserVo detail(@PathVariable("uid") Long uid);

    /** 按 uid 列表批量取用户视图；不存在的 uid 不出现在结果里 */
    @PostMapping("/batch")
    List<AdminUserVo> batch(@RequestBody List<Long> uids);

    /** 更新账号状态（0 正常 / 1 封禁） */
    @PutMapping("/{uid}/status")
    void updateStatus(@PathVariable("uid") Long uid, @RequestParam("status") Integer status);

    /** 更新账号角色（0 普通 / 1 管理员 / 2 超级管理员） */
    @PutMapping("/{uid}/role")
    void updateRole(@PathVariable("uid") Long uid, @RequestParam("role") Integer role);

    /** 重置密码：写 bcrypt 哈希与 password_updated_at，并吊销目标用户全部会话；返回更新行数 */
    @PostMapping("/{uid}/password/reset")
    int resetPassword(@PathVariable("uid") Long uid, @RequestBody AdminPasswordResetVo body);

    /** 按完整 URL 查用户头像/背景引用，存储对象删除前校验用 */
    @GetMapping("/refs")
    List<AdminUserRefVo> refs(@RequestParam("url") String url);
}
