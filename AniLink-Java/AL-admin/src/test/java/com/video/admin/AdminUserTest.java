package com.video.admin;

import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserProfileVo;
import com.video.common.exception.VideoException;
import com.video.common.utils.PasswordUtil;
import com.video.admin.mapper.AdminUserMapper;
import com.video.admin.service.impl.AdminUserServiceImpl;
import com.video.admin.vo.AdminResetPasswordVo;
import com.video.admin.vo.AdminUserVo;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户封禁与角色调整规则。 */
@ExtendWith(MockitoExtension.class)
class AdminUserTest {

    @Mock
    private AdminUserMapper adminUserMapper;
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
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));

        VideoException error = assertThrows(VideoException.class,
                () -> service.updateRole(1L, 2L, 1));
        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updateRole(2L, 1);
    }

    @Test
    void superAdminCanChangeRole() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 2));
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
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 0));
        when(adminUserMapper.updatePassword(eq(2L), anyString())).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(adminUserMapper).updatePassword(eq(2L), hash.capture());
        assertTrue(PasswordUtil.matches(result.getTempPassword(), hash.getValue()));
        assertFalse(result.getTempPassword().equals(hash.getValue()));
        verify(userInternalClient).revokeSessions(2L);
    }

    @Test
    void normalAdminCannotResetAdminPassword() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 1));
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 1));

        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 2L));

        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updatePassword(eq(2L), anyString());
    }

    @Test
    void superAdminCanResetAdminPassword() {
        when(userInternalClient.profile(1L)).thenReturn(profile(1L, 2));
        when(adminUserMapper.getAdminDetail(2L)).thenReturn(userVo(2L, "u2", 1));
        when(adminUserMapper.updatePassword(eq(2L), anyString())).thenReturn(1);

        AdminResetPasswordVo result = service.resetPassword(1L, 2L);

        assertEquals(12, result.getTempPassword().length());
        verify(userInternalClient).revokeSessions(2L);
    }

    @Test
    void cannotResetOwnPassword() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.resetPassword(1L, 1L));

        assertEquals(309, error.getCode());
        verify(adminUserMapper, never()).updatePassword(eq(1L), anyString());
    }

    private UserProfileVo profile(Long uid, int role) {
        UserProfileVo vo = new UserProfileVo();
        vo.setUid(uid);
        vo.setUsername("u" + uid);
        vo.setNickname("n" + uid);
        vo.setRole(role);
        return vo;
    }

    private AdminUserVo userVo(Long uid, String name, int role) {
        AdminUserVo vo = new AdminUserVo();
        vo.setUid(uid);
        vo.setUsername(name);
        vo.setRole(role);
        return vo;
    }
}
