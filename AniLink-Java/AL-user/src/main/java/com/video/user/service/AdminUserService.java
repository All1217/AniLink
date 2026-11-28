package com.video.user.service;

import com.video.api.user.dto.AdminUserPageVo;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.api.user.dto.AdminUserVo;

import java.util.List;

/**
 * 用户域管理契约服务：AL-admin 的用户查询与状态/角色/密码变更都走这里。
 * 管理端的角色校验属于 admin 业务规则，不在本服务内实现。
 */
public interface AdminUserService {

    /** 用户分页：关键字匹配 username/nickname/uid，status、role 为精确过滤 */
    AdminUserPageVo page(long current, long size, String keyword, Integer status, Integer role);

    /** 用户详情（不含密码）；不存在返回 null */
    AdminUserVo detail(Long uid);

    /** 按 uid 列表批量取用户视图；不存在的 uid 不出现在结果里 */
    List<AdminUserVo> listByUids(List<Long> uids);

    /** 更新账号状态（0 正常 / 1 封禁） */
    void updateStatus(Long uid, Integer status);

    /** 更新账号角色（0 普通 / 1 管理员 / 2 超级管理员） */
    void updateRole(Long uid, Integer role);

    /** 重置密码：写 bcrypt 哈希与 password_updated_at，并吊销目标用户全部会话；返回更新行数 */
    int resetPassword(Long uid, String rawPassword);

    /** 按完整 URL 查用户头像/背景引用 */
    List<AdminUserRefVo> refsByUrl(String url);
}
