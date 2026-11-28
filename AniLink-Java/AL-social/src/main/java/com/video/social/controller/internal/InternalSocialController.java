package com.video.social.controller.internal;

import com.video.api.social.dto.UserFollowStatsVo;
import com.video.social.service.FansService;
import com.video.social.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社交域内部契约：只给其他服务调用（网关不暴露 /internal/**），返回裸值便于 Feign 直接反序列化。
 */
@Tag(name = "内部-社交域")
@RestController
@RequestMapping("/internal/v1/social")
@RequiredArgsConstructor
public class InternalSocialController {

    private final FavoriteService favoriteService;
    private final FansService fansService;

    @Operation(summary = "确保用户默认收藏夹存在（幂等）")
    @PostMapping("/favorites/default/{uid}")
    public Boolean ensureDefaultFavorite(@PathVariable("uid") Long uid) {
        return favoriteService.ensureDefaultFavorite(uid);
    }

    @Operation(summary = "取用户粉丝 id 集合")
    @GetMapping("/fans/{uid}/ids")
    public List<Long> fansIds(@PathVariable("uid") Long uid) {
        return fansService.listFansIds(uid);
    }

    @Operation(summary = "取用户关注统计（粉丝数/关注数）")
    @GetMapping("/users/{uid}/stats")
    public UserFollowStatsVo userStats(@PathVariable("uid") Long uid) {
        return fansService.userStats(uid);
    }
}
