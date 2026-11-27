package com.video.user.adapter;

import com.video.common.result.Result;
import com.video.user.vo.UserStatsCountsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Phase 1 过渡：统计聚合与默认收藏夹仍属 AL-web 的表，通过内部接口调用。
 * Phase 3/4 各域迁走后删除本客户端，改调 al-social/al-video 的契约。
 */
@FeignClient(name = "legacy-user-support", url = "${al.user.legacy-support-url}")
public interface LegacyUserSupportClient {

    @GetMapping("/internal/v1/users/{uid}/stats")
    Result<UserStatsCountsVo> stats(@PathVariable("uid") Long uid);

    @PostMapping("/internal/v1/users/{uid}/default-favorite")
    Result<Boolean> ensureDefaultFavorite(@PathVariable("uid") Long uid);
}
