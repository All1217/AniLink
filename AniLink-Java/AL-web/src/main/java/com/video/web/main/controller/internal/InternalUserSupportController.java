package com.video.web.main.controller.internal;

import com.video.common.result.Result;
import com.video.web.main.service.impl.InternalUserSupportService;
import com.video.web.main.vo.UserStatsCountsVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：仅供 al-user 在 Phase 1 过渡期调用。
 * 网关不声明 /internal/** 路由，外部不可达；Phase 4 各业务域迁走后整包删除。
 * 默认收藏夹创建已迁到 al-social（/internal/v1/social/favorites/default/{uid}）。
 */
@Tag(name = "内部-用户域过渡适配")
@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserSupportController {

    private final InternalUserSupportService supportService;

    public InternalUserSupportController(InternalUserSupportService supportService) {
        this.supportService = supportService;
    }

    @Operation(summary = "用户统计聚合（粉丝/关注/获赞/播放/投稿/收藏）")
    @GetMapping("/{uid}/stats")
    public Result<UserStatsCountsVo> stats(@PathVariable Long uid) {
        return Result.ok(supportService.statsOf(uid));
    }
}
