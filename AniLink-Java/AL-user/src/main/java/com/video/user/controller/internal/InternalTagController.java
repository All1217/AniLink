package com.video.user.controller.internal;

import com.video.api.user.dto.CommonTagsQueryVo;
import com.video.api.user.dto.TagUsersQueryVo;
import com.video.api.user.dto.UserTagVo;
import com.video.user.service.UserTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户标签内部契约实现：只给其他服务调用（网关不暴露 /internal/**）。
 * 返回裸值而不是 Result，便于 Feign 直接反序列化。
 */
@Tag(name = "内部-用户标签")
@RestController
@RequestMapping("/internal/v1/users/tags")
public class InternalTagController {

    private final UserTagService userTagService;

    public InternalTagController(UserTagService userTagService) {
        this.userTagService = userTagService;
    }

    @Operation(summary = "取两个用户的共同标签")
    @PostMapping("/common")
    public List<String> commonTags(@RequestBody CommonTagsQueryVo query) {
        return userTagService.commonTags(query.getUid(), query.getVid());
    }

    @Operation(summary = "批量取用户标签")
    @PostMapping("/batch")
    public List<UserTagVo> batch(@RequestBody List<Long> uids) {
        return userTagService.batchTags(uids);
    }

    @Operation(summary = "按标签名取权重大于 0 的用户 id")
    @PostMapping("/uids")
    public List<Long> uidsByTag(@RequestBody TagUsersQueryVo query) {
        return userTagService.uidsByTag(query.getTagName());
    }
}
