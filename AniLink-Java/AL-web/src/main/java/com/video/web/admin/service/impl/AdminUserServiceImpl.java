package com.video.web.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import com.video.model.entity.UserInfo;
import com.video.web.admin.mapper.AdminUserMapper;
import com.video.web.admin.service.AdminUserService;
import com.video.web.admin.vo.AdminUserQueryVo;
import com.video.web.admin.vo.AdminUserVo;
import com.video.web.main.service.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 用户管理：封禁/解封与超管角色调整。 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final UserInfoService userInfoService;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper, UserInfoService userInfoService) {
        this.adminUserMapper = adminUserMapper;
        this.userInfoService = userInfoService;
    }

    @Override
    public IPage<AdminUserVo> page(AdminUserQueryVo query) {
        long current = Math.max(1, query.getCurrent());
        long size = Math.min(200, Math.max(1, query.getSize()));
        return adminUserMapper.pageAdmin(new Page<>(current, size), query);
    }

    @Override
    public AdminUserVo detail(Long uid) {
        AdminUserVo vo = adminUserMapper.getAdminDetail(uid);
        if (vo == null) {
            throw new VideoException(404, "用户不存在");
        }
        return vo;
    }

    @Override
    public void updateStatus(Long operatorUid, Long uid, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new VideoException(202, "status 只能是 0 或 1");
        }
        if (operatorUid.equals(uid)) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), "不能封禁自己");
        }
        if (adminUserMapper.getAdminDetail(uid) == null) {
            throw new VideoException(404, "用户不存在");
        }
        adminUserMapper.updateStatus(uid, status);
        log.info("admin action operator={} action=user.status uid={} to={}",
                operatorUid, uid, status);
    }

    @Override
    public void updateRole(Long operatorUid, Long uid, Integer role) {
        if (role == null || role < 0 || role > 2) {
            throw new VideoException(202, "role 只能是 0/1/2");
        }
        if (operatorUid.equals(uid)) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), "不能修改自己的角色");
        }
        UserInfo operator = userInfoService.getById(operatorUid);
        if (operator == null || operator.getRole() == null || operator.getRole() != 2) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        if (adminUserMapper.getAdminDetail(uid) == null) {
            throw new VideoException(404, "用户不存在");
        }
        adminUserMapper.updateRole(uid, role);
        log.info("admin action operator={} action=user.role uid={} to={}",
                operatorUid, uid, role);
    }
}
