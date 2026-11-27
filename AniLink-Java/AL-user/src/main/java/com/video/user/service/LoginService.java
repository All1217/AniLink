package com.video.user.service;

import com.video.model.entity.UserInfo;
import com.video.user.vo.ChangePasswordVo;
import com.video.user.vo.LoginVo;

public interface LoginService {
    String login(LoginVo loginVo);

    UserInfo getLoginUserInfo(Long userId);

    String refreshToken(String refreshToken);

    void logout();

    /** 签发一对令牌（access 返回 + refresh 写 Cookie），供注册后直接登录复用 */
    String issueTokens(Long uid, String username);

    /** 修改密码：校验旧密码 → 更新哈希 → 吊销该用户全部会话 */
    void changePassword(Long uid, ChangePasswordVo changePasswordVo);

    /** 管理员重置密码后吊销目标用户的全部 refresh 会话 */
    void revokeAllSessions(Long uid);
}
