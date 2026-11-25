package com.video.contract;

import com.video.common.exception.GlobalExceptionHandler;
import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import com.video.web.main.controller.login.LoginController;
import com.video.web.main.service.LoginService;
import com.video.web.main.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@Import(GlobalExceptionHandler.class)
class LoginContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginService loginService;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void loginReturnsWrappedAccessToken() throws Exception {
        when(loginService.login(any())).thenReturn("access-token-123");

        mockMvc.perform(post("/main/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("login-success.json"), true));
    }

    @Test
    void loginFailureReturns307() throws Exception {
        when(loginService.login(any())).thenThrow(new VideoException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR));

        mockMvc.perform(post("/main/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("login-failure-307.json"), true));
    }

    @Test
    void refreshWithoutCookieReturns601() throws Exception {
        mockMvc.perform(get("/main/public/refresh"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("refresh-missing-601.json"), true));
    }
}
