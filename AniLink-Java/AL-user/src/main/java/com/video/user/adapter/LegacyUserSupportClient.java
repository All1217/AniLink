package com.video.user.adapter;

import com.video.common.result.Result;
import com.video.user.vo.UserStatsCountsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Phase 1 过渡：用户统计聚合仍由 AL-web 查自己的表，通过内部接口调用。
 * Phase 4 用户统计切到数据中心后删除本客户端。
 */
@FeignClient(name = "legacy-user-support", url = "${al.user.legacy-support-url}")
public interface LegacyUserSupportClient {

    @GetMapping("/internal/v1/users/{uid}/stats")
    Result<UserStatsCountsVo> stats(@PathVariable("uid") Long uid);
}
