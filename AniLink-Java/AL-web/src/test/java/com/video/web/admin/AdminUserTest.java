package com.video.web.admin;

import com.video.common.exception.VideoException;
import com.video.common.utils.PasswordUtil;
import com.video.model.entity.UserInfo;
import com.video.web.admin.mapper.AdminUserMapper;
import com.video.web.admin.service.impl.AdminUserServiceImpl;
import com.video.web.admin.vo.AdminResetPasswordVo;
import com.video.web.admin.vo.AdminUserVo;
import com.video.web.main.service.LoginService;
import com.video.web.main.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户封禁与角色调整规则。 */
@ExtendWith(MockitoExtension.class)
class AdminUserTest {

    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private UserInfoService userInfoService;
    @Mock
    private LoginService loginService;
    @InjectMocks
    private AdminUserServiceImpl service;

    @Test
    void banUser() {
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(new AdminUserVo());
        service.updateStatus(1L, 2L, 1);
        verify(adminUserMapper).updateStatus(2L, 1);
    }

    @Test
    void cannotBanSelf() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateStatus(1L, 1L, 1));
        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updateStatus(1L, 1);
    }

    @Test
    void adminCannotChangeRole() {
        UserInfo operator = new UserInfo();
        operator.setUid(1L);
        operator.setRole(1);
        when(userInfoService.getById(1L)).thenReturn(operator);

        VideoException error = assertThrows(VideoException.class,
                () -> service.updateRole(1L, 2L, 1));
        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updateRole(2L, 1);
    }

    @Test
    void superAdminCanChangeRole() {
        UserInfo operator = new UserInfo();
        operator.setUid(1L);
        operator.setRole(2);
        when(userInfoService.getById(1L)).thenReturn(operator);
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(new AdminUserVo());

        service.updateRole(1L, 2L, 1);
        verify(adminUserMapper).updateRole(2L, 1);
    }

    @Test
    void cannotChangeOwnRole() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateRole(1L, 1L, 0));
        assertEquals(309, error.getCode());
    }

    @Test
    void invalidRoleRejected() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateRole(1L, 2L, 3));
        assertEquals(202, error.getCode());
    }

    @Test
    void normalAdminCanResetNormalUserPassword() {
        UserInfo operator = user(1L, 1);
        when(userInfoService.getById(1L)).thenReturn(operator);
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 0));
        when(adminUserMapper.updatePassword(eq(2L), anyString())).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(adminUserMapper).updatePassword(eq(2L), hash.capture());
        assertTrue(PasswordUtil.matches(result.getTempPassword(), hash.getValue()));
        assertFalse(result.getTempPassword().equals(hash.getValue()));
        verify(loginService).revokeAllSessions(2L);
    }

    @Test
    void normalAdminCannotResetAdminPassword() {
        when(userInfoService.getById(1L)).thenReturn(user(1L, 1));
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 1));

        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 2L));

        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updatePassword(eq(2L), anyString());
    }

    @Test
    void superAdminCanResetAdminPassword() {
        when(userInfoService.getById(1L)).thenReturn(user(1L, 2));
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 1));
        when(adminUserMapper.updatePassword(eq(2L), anyString())).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        verify(loginService).revokeAllSessions(2L);
    }

    @Test
    void cannotResetOwnPassword() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 1L));

        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updatePassword(eq(1L), anyString());
    }

    private UserInfo user(Long uid, int role) {
        UserInfo user = new UserInfo();
        user.setUid(uid);
        user.setRole(role);
        return user;
    }

    private AdminUserVo userVo(Long uid, String username, int role) {
        AdminUserVo user = new AdminUserVo();
        user.setUid(uid);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }
}
