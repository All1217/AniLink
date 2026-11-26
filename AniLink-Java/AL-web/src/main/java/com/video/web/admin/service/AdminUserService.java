package com.video.web.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.web.admin.vo.AdminResetPasswordVo;
import com.video.web.admin.vo.AdminUserQueryVo;
import com.video.web.admin.vo.AdminUserVo;

/** 管理端用户查询、封禁与角色调整。 */
public interface AdminUserService {
    IPage<AdminUserVo> page(AdminUserQueryVo query);

    AdminUserVo detail(Long uid);

    void updateStatus(Long operatorUid, Long uid, Integer status);

    void updateRole(Long operatorUid, Long uid, Integer role);

    /** 重置目标用户密码，返回仅显示一次的临时密码。 */
    AdminResetPasswordVo resetPassword(Long operatorUid, Long uid);
}
