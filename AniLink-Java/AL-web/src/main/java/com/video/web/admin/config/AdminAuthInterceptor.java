package com.video.web.admin.config;

import com.video.common.exception.VideoException;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.common.result.ResultCodeEnum;
import com.video.model.entity.UserInfo;
import com.video.web.main.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 管理接口的二次角色校验：网关只保证登录，这里保证 role>=1。 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final UserInfoService userInfoService;

    public AdminAuthInterceptor(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new VideoException(ResultCodeEnum.ADMIN_LOGIN_AUTH);
        }
        UserInfo user = userInfoService.getById(loginUser.getUserId());
        if (user == null || user.getRole() == null || user.getRole() < 1) {
            throw new VideoException(ResultCodeEnum.ADMIN_ACCESS_FORBIDDEN);
        }
        return true;
    }
}
