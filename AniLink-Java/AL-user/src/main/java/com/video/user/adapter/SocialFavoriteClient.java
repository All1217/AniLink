package com.video.user.adapter;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 社交域内部契约：注册时幂等创建默认收藏夹。
 *
 * <p>favorite 表自 Phase 3 起由 al-social 接管，AL-web 过渡适配层不再写该表，
 * 这里直接调用 al-social 的内部接口（网关不暴露 /internal/**）。</p>
 */
@FeignClient(name = "socialservice", contextId = "socialFavoriteClient",
        url = "${al.user.social-url:http://localhost:8031}", path = "/internal/v1/social")
public interface SocialFavoriteClient {

    /** 确保默认收藏夹存在，已存在返回 false */
    @PostMapping("/favorites/default/{uid}")
    Boolean ensureDefaultFavorite(@PathVariable("uid") Long uid);
}
