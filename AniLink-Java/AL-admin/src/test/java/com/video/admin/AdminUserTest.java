package com.video.admin;

import com.video.admin.service.impl.AdminUserServiceImpl;
import com.video.admin.vo.AdminResetPasswordVo;
import com.video.api.user.AdminUserInternalClient;
import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.AdminPasswordResetVo;
import com.video.api.user.dto.UserProfileVo;
import com.video.common.exception.VideoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户封禁与角色调整规则；用户读写改走 al-user 内部契约。 */
@ExtendWith(MockitoExtension.class)
class AdminUserTest {

    @Mock
    private AdminUserInternalClient adminUserClient;
    @Mock
    private ObjectProvider<UserInternalClient> clientProvider;
    @Mock
    private UserInternalClient userInternalClient;
    @InjectMocks
    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(clientProvider.getObject()).thenReturn(userInternalClient);
    }

    @Test
    void banUser() {
        when(adminUserClient.detail(2L)).thenReturn(userVo(2L, "u2", 0));
        service.updateStatus(1L, 2L, 1);
        verify(adminUserClient).updateStatus(2L, 1);
    }

    @Test
    void cannotBanSelf() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateStatus(1L, 1L, 1));
        assertEquals(309, error.getCode());
        verify(adminUserClient, never()).updateStatus(1L, 1);
    }

    @Test
    void adminCannotChangeRole() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));

        VideoException error = assertThrows(VideoException.class,
                () -> service.updateRole(1L, 2L, 1));
        assertEquals(309, error.getCode());
        verify(adminUserClient, never()).updateRole(2L, 1);
    }

    @Test
    void superAdminCanChangeRole() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 2));
        when(adminUserClient.detail(2L)).thenReturn(userVo(2L, "u2", 0));

        service.updateRole(1L, 2L, 1);
        verify(adminUserClient).updateRole(2L, 1);
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
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));
        when(adminUserClient.detail(2L)).thenReturn(userVo(2L, "u2", 0));
        when(adminUserClient.resetPassword(eq(2L), any(AdminPasswordResetVo.class))).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        // 明文临时密码只在内网请求体里传输，bcrypt 哈希由 al-user 负责
        ArgumentCaptor<AdminPasswordResetVo> body = ArgumentCaptor.forClass(AdminPasswordResetVo.class);
        verify(adminUserClient).resetPassword(eq(2L), body.capture());
        assertEquals(result.getTempPassword(), body.getValue().getPassword());
        assertFalse(result.getTempPassword().isEmpty());
    }

    @Test
    void normalAdminCannotResetAdminPassword() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));
        when(adminUserClient.detail(2L)).thenReturn(userVo(2L, "u2", 1));

        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 2L));

        assertEquals(309, error.getCode());
        verify(adminUserClient, never()).resetPassword(eq(2L), any(AdminPasswordResetVo.class));
    }

    @Test
    void superAdminCanResetAdminPassword() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 2));
        when(adminUserClient.detail(2L)).thenReturn(userVo(2L, "u2", 1));
        when(adminUserClient.resetPassword(eq(2L), any(AdminPasswordResetVo.class))).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        verify(adminUserClient).resetPassword(eq(2L), any(AdminPasswordResetVo.class));
    }

    @Test
    void cannotResetOwnPassword() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 1L));

        assertEquals(309, error.getCode());
        verify(adminUserClient, never()).resetPassword(eq(1L), any(AdminPasswordResetVo.class));
    }

    private UserProfileVo profile(Long uid, int role) {
        UserProfileVo vo = new UserProfileVo();
        vo.setUid(uid);
        vo.setUsername("u" + uid);
        vo.setNickname("n" + uid);
        vo.setRole(role);
        return vo;
    }

    private com.video.api.user.dto.AdminUserVo userVo(Long uid, String name, int role) {
        com.video.api.user.dto.AdminUserVo vo = new com.video.api.user.dto.AdminUserVo();
        vo.setUid(uid);
        vo.setUsername(name);
        vo.setRole(role);
        return vo;
    }
}
