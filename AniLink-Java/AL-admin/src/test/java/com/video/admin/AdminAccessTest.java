package com.video.admin;

import com.video.api.user.UserInternalClient;
import com.video.api.user.dto.UserProfileVo;
import com.video.api.user.dto.UserRoleVo;
import com.video.common.exception.GlobalExceptionHandler;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.admin.config.AdminAuthConfig;
import com.video.admin.config.AdminAuthInterceptor;
import com.video.admin.controller.AdminAuthController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 管理接口角色校验：305 未登录、309 普通用户、200 管理员。 */
@WebMvcTest(AdminAuthController.class)
@Import({AdminAuthConfig.class, AdminAuthInterceptor.class, GlobalExceptionHandler.class})
class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserInternalClient userInternalClient;

    @AfterEach
    void clearHolder() {
        LoginUserHolder.clear();
    }

    @Test
    void missingLoginReturns305() throws Exception {
        mockMvc.perform(get("/main/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(305));
    }

    @Test
    void normalUserReturns309() throws Exception {
        LoginUserHolder.setLoginUser(new LoginUser(1L, "alice"));
        when(userInternalClient.role(1L)).thenReturn(new UserRoleVo(1L, 0));

        mockMvc.perform(get("/main/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(309));
    }

    @Test
    void adminCanReadMe() throws Exception {
        LoginUserHolder.setLoginUser(new LoginUser(2L, "admin"));
        when(userInternalClient.role(2L)).thenReturn(new UserRoleVo(2L, 1));
        when(userInternalClient.profile(2L)).thenReturn(profile(2L, 1));

        mockMvc.perform(get("/main/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uid").value(2))
                .andExpect(jsonPath("$.data.role").value(1));
    }

    private UserProfileVo profile(Long uid, int role) {
        UserProfileVo vo = new UserProfileVo();
        vo.setUid(uid);
        vo.setUsername("u" + uid);
        vo.setNickname("n" + uid);
        vo.setRole(role);
        return vo;
    }
}
