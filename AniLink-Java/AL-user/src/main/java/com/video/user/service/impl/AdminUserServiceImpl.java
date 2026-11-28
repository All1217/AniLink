package com.video.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.api.user.dto.AdminUserPageVo;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.api.user.dto.AdminUserVo;
import com.video.common.exception.VideoException;
import com.video.common.utils.PasswordUtil;
import com.video.model.entity.UserInfo;
import com.video.user.mapper.UserInfoMapper;
import com.video.user.service.AdminUserService;
import com.video.user.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户域管理契约实现：管理端不再直连 user 表，所有读写收敛到本服务。
 * 只返回管理页需要字段，密码哈希不出服务边界。
 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {

    /** 分页大小上限，避免内部调用方一次拉全表 */
    private static final long MAX_PAGE_SIZE = 200L;

    private final UserInfoMapper userInfoMapper;
    private final LoginService loginService;

    public AdminUserServiceImpl(UserInfoMapper userInfoMapper, LoginService loginService) {
        this.userInfoMapper = userInfoMapper;
        this.loginService = loginService;
    }

    @Override
    public AdminUserPageVo page(long current, long size, String keyword, Integer status, Integer role) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        LambdaQueryWrapper<UserInfo> wrapper = Wrappers.lambdaQuery();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        // 关键字语义与原管理端一致：username / nickname 模糊，或 uid 精确
        wrapper.and(hasKeyword, w -> w.like(UserInfo::getUsername, keyword)
                        .or().like(UserInfo::getNickname, keyword)
                        .or().apply("cast(uid as char) = {0}", keyword))
                .eq(status != null, UserInfo::getStatus, status)
                .eq(role != null, UserInfo::getRole, role)
                .orderByDesc(UserInfo::getCreateDate)
                .orderByDesc(UserInfo::getUid);
        IPage<UserInfo> result = userInfoMapper.selectPage(new Page<>(safeCurrent, safeSize), wrapper);
        AdminUserPageVo vo = new AdminUserPageVo();
        vo.setRecords(result.getRecords().stream().map(this::toVo).toList());
        vo.setTotal(result.getTotal());
        vo.setCurrent(result.getCurrent());
        vo.setSize(result.getSize());
        return vo;
    }

    @Override
    public AdminUserVo detail(Long uid) {
        if (uid == null) {
            return null;
        }
        // selectById 会跳过 @TableField(select = false) 的 password 列
        return toVo(userInfoMapper.selectById(uid));
    }

    @Override
    public List<AdminUserVo> listByUids(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        return userInfoMapper.selectBatchIds(uids).stream().map(this::toVo).toList();
    }

    @Override
    public void updateStatus(Long uid, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new VideoException(202, "status 只能是 0 或 1");
        }
        userInfoMapper.updateStatus(uid, status);
    }

    @Override
    public void updateRole(Long uid, Integer role) {
        if (role == null || role < 0 || role > 2) {
            throw new VideoException(202, "role 只能是 0/1/2");
        }
        userInfoMapper.updateRole(uid, role);
    }

    @Override
    public int resetPassword(Long uid, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new VideoException(202, "新密码不能为空");
        }
        // 密码哈希是用户域职责：入参为临时密码明文，落库前统一 bcrypt
        int updated = userInfoMapper.updatePassword(uid, PasswordUtil.encode(rawPassword));
        if (updated == 1) {
            try {
                loginService.revokeAllSessions(uid);
            } catch (Exception e) {
                // 密码已更新成功，会话吊销失败只记录，避免调用方误判密码未重置
                log.warn("重置密码后吊销用户 {} 会话失败", uid, e);
            }
            log.info("admin contract action=user.password.reset uid={}", uid);
        }
        return updated;
    }

    @Override
    public List<AdminUserRefVo> refsByUrl(String url) {
        if (url == null || url.isBlank()) {
            return List.of();
        }
        return userInfoMapper.findUserRefs(url);
    }

    /** UserInfo → 管理端视图，只搬运管理页字段 */
    private AdminUserVo toVo(UserInfo user) {
        if (user == null) {
            return null;
        }
        AdminUserVo vo = new AdminUserVo();
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRole(user.getRole());
        vo.setCreateDate(user.getCreateDate());
        return vo;
    }
}
