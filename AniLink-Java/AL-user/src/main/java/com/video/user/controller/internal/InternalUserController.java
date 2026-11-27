package com.video.user.controller.internal;

import com.video.api.user.dto.UserBriefVo;
import com.video.api.user.dto.UserProfileVo;
import com.video.api.user.dto.UserRoleVo;
import com.video.api.user.dto.UserStatsVo;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户域内部契约实现，只给其他服务调用（网关不暴露 /internal/**）。
 * 返回裸 DTO 而不是 Result，便于 Feign 直接反序列化。
 */
@Tag(name = "内部-用户域")
@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserController {

    private final UserInfoService userInfoService;
    private final com.video.user.service.LoginService loginService;

    public InternalUserController(UserInfoService userInfoService,
                                  com.video.user.service.LoginService loginService) {
        this.userInfoService = userInfoService;
        this.loginService = loginService;
    }

    @Operation(summary = "吊销用户全部 refresh 会话")
    @PostMapping("/{uid}/sessions/revoke")
    public void revokeSessions(@PathVariable("uid") Long uid) {
        loginService.revokeAllSessions(uid);
    }

    @Operation(summary = "批量取用户摘要")
    @PostMapping("/batch")
    public List<UserBriefVo> batch(@RequestBody List<Long> uids) {
        return userInfoService.listBrief(uids);
    }

    @Operation(summary = "取账号资料（不存在返回 null）")
    @GetMapping("/{uid}/profile")
    public UserProfileVo profile(@PathVariable("uid") Long uid) {
        UserInfo user = userInfoService.getById(uid);
        if (user == null) {
            return null;
        }
        UserProfileVo vo = new UserProfileVo();
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Operation(summary = "取账号角色（不存在返回 null）")
    @GetMapping("/{uid}/role")
    public UserRoleVo role(@PathVariable("uid") Long uid) {
        UserInfo user = userInfoService.getById(uid);
        return user == null ? null : new UserRoleVo(uid, user.getRole());
    }

    @Operation(summary = "取用户统计（不存在返回 null）")
    @GetMapping("/{uid}/stats")
    public UserStatsVo stats(@PathVariable("uid") Long uid) {
        UserStats stats = userInfoService.getUserStatsByUid(uid);
        if (stats == null) {
            return null;
        }
        UserStatsVo vo = new UserStatsVo();
        vo.setUid(stats.getUid());
        vo.setFansCount(stats.getFansCount());
        vo.setFollowCount(stats.getFollowCount());
        vo.setLikeCount(stats.getLikeCount());
        vo.setPlayCount(stats.getPlayCount());
        vo.setVideoCount(stats.getVideoCount());
        vo.setCollectCount(stats.getCollectCount());
        return vo;
    }
}
