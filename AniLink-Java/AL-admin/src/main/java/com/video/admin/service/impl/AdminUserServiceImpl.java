package com.video.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import com.video.common.utils.PasswordUtil;
import com.video.model.entity.UserInfo;
import com.video.admin.mapper.AdminUserMapper;
import com.video.admin.service.AdminUserService;
import com.video.admin.vo.AdminResetPasswordVo;
import com.video.admin.vo.AdminUserQueryVo;
import com.video.admin.vo.AdminUserVo;
import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserProfileVo;
import org.springframework.beans.factory.ObjectProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/** 用户管理：封禁/解封、超管角色调整与密码重置。 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String ALL = UPPER + LOWER + DIGIT;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminUserMapper adminUserMapper;
    private final ObjectProvider<UserInternalClient> clientProvider;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper,
                                ObjectProvider<UserInternalClient> clientProvider) {
        this.adminUserMapper = adminUserMapper;
        this.clientProvider = clientProvider;
    }

    /** 用户资料已归 al-user，经内部接口读取；失败按无权限处理 */
    private UserProfileVo profileOf(Long uid) {
        try {
            return clientProvider.getObject().profile(uid);
        } catch (Exception e) {
            log.error("读取用户资料失败：uid={}", uid, e);
            return null;
        }
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
        UserProfileVo operator = profileOf(operatorUid);
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

    @Override
    public AdminResetPasswordVo resetPassword(Long operatorUid, Long uid) {
        if (operatorUid.equals(uid)) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(), "不能重置自己的密码");
        }
        UserProfileVo operator = profileOf(operatorUid);
        if (operator == null || operator.getRole() == null || operator.getRole() < 1) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        AdminUserVo target = adminUserMapper.getAdminDetail(uid);
        if (target == null) {
            throw new VideoException(404, "用户不存在");
        }
        if (target.getRole() != null && target.getRole() >= 1 && operator.getRole() != 2) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN.getCode(),
                    "只有超级管理员可以重置管理员密码");
        }
        String tempPassword = generateTempPassword();
        if (adminUserMapper.updatePassword(uid, PasswordUtil.encode(tempPassword)) != 1) {
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "重置密码失败");
        }
        try {
            clientProvider.getObject().revokeSessions(uid);
        } catch (Exception e) {
            // 密码已经更新成功，会话吊销失败只记录，避免临时密码无法返回
            log.warn("重置密码后吊销用户 {} 会话失败", uid, e);
        }
        log.info("admin action operator={} action=user.password.reset uid={}", operatorUid, uid);
        AdminResetPasswordVo result = new AdminResetPasswordVo();
        result.setUid(uid);
        result.setUsername(target.getUsername());
        result.setTempPassword(tempPassword);
        return result;
    }

    /** 12 位临时密码：至少包含大写、小写、数字，排除易混淆字符。 */
    private String generateTempPassword() {
        StringBuilder builder = new StringBuilder(12);
        builder.append(pick(UPPER)).append(pick(LOWER)).append(pick(DIGIT));
        for (int i = 3; i < 12; i++) {
            builder.append(pick(ALL));
        }
        return builder.toString();
    }

    private char pick(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }
}
