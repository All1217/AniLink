package com.video.web.admin;

import com.video.common.exception.GlobalExceptionHandler;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.model.entity.UserInfo;
import com.video.web.admin.config.AdminAuthConfig;
import com.video.web.admin.config.AdminAuthInterceptor;
import com.video.web.admin.controller.AdminAuthController;
import com.video.web.main.service.UserInfoService;
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
    private UserInfoService userInfoService;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

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
        when(userInfoService.getById(1L)).thenReturn(user(1L, 0));

        mockMvc.perform(get("/main/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(309));
    }

    @Test
    void adminCanReadMe() throws Exception {
        LoginUserHolder.setLoginUser(new LoginUser(2L, "admin"));
        when(userInfoService.getById(2L)).thenReturn(user(2L, 1));

        mockMvc.perform(get("/main/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uid").value(2))
                .andExpect(jsonPath("$.data.role").value(1));
    }

    private UserInfo user(Long uid, int role) {
        UserInfo user = new UserInfo();
        user.setUid(uid);
        user.setUsername("u" + uid);
        user.setNickname("n" + uid);
        user.setRole(role);
        return user;
    }
}
