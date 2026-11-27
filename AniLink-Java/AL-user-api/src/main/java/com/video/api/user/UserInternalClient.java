package com.video.api.user;

import com.video.api.user.dto.UserBriefVo;
import com.video.api.user.dto.UserProfileVo;
import com.video.api.user.dto.UserRoleVo;
import com.video.api.user.dto.UserStatsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 用户域内部契约：只给其他服务调用，网关不暴露 /internal/**。
 * 调用方请使用批量接口并在本地做短 TTL 缓存，禁止在循环里逐条调用。
 */
@FeignClient(name = "userservice", contextId = "userInternalClient",
        url = "${al.user.service-url:}", path = "/internal/v1/users")
public interface UserInternalClient {

    /** 批量取用户摘要；不存在的 id 直接不出现在结果里 */
    @PostMapping("/batch")
    List<UserBriefVo> batch(@RequestBody List<Long> uids);

    /** 取单个用户统计；用户不存在时返回 null */
    @GetMapping("/{uid}/stats")
    UserStatsVo stats(@PathVariable("uid") Long uid);

    /** 取账号角色，管理端鉴权用；用户不存在时返回 null */
    @GetMapping("/{uid}/role")
    UserRoleVo role(@PathVariable("uid") Long uid);

    /** 取账号资料（登录名/昵称/角色），管理端身份接口用 */
    @GetMapping("/{uid}/profile")
    UserProfileVo profile(@PathVariable("uid") Long uid);

    /** 吊销该用户全部 refresh 会话，改密/重置密码后调用 */
    @PostMapping("/{uid}/sessions/revoke")
    void revokeSessions(@PathVariable("uid") Long uid);
}
