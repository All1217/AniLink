package com.video.web.admin;

import com.video.common.exception.VideoException;
import com.video.model.entity.UserInfo;
import com.video.web.admin.mapper.AdminUserMapper;
import com.video.web.admin.service.impl.AdminUserServiceImpl;
import com.video.web.admin.vo.AdminUserVo;
import com.video.web.main.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
