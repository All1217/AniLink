package com.video.api.social;

import com.video.api.social.dto.UserFollowStatsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 社交域内部契约：只给其他服务调用，网关不暴露 /internal/**。
 * 调用方在循环里取数时应先缓存，避免 Feign 放大。
 */
@FeignClient(name = "socialservice", contextId = "socialInternalClient",
        url = "${al.social.service-url:}", path = "/internal/v1/social")
public interface SocialInternalClient {

    /** 取该用户的粉丝 id 集合；没有粉丝返回空列表 */
    @GetMapping("/fans/{uid}/ids")
    List<Long> fansIds(@PathVariable("uid") Long uid);

    /** 取该用户的关注统计（粉丝数/关注数）；用户不存在时两者为 0 */
    @GetMapping("/users/{uid}/stats")
    UserFollowStatsVo userStats(@PathVariable("uid") Long uid);
}
