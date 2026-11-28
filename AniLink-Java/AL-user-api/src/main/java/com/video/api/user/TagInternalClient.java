package com.video.api.user;

import com.video.api.user.dto.CommonTagsQueryVo;
import com.video.api.user.dto.TagUsersQueryVo;
import com.video.api.user.dto.UserTagVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 用户标签内部契约：user_tag 自 Phase 4 起归 al-user，数据中心的推荐/标签接口通过本契约取标签。
 * 只暴露单表查询能力；标签与弹幕的聚合由调用方（数据中心 BFF）在内存完成，避免跨域 JOIN。
 */
@FeignClient(name = "userservice", contextId = "tagInternalClient",
        url = "${al.user.service-url:}", path = "/internal/v1/users/tags")
public interface TagInternalClient {

    /** 取两个用户的共同标签名 */
    @PostMapping("/common")
    List<String> commonTags(@RequestBody CommonTagsQueryVo query);

    /** 批量取用户标签行；不存在的用户不出现在结果里 */
    @PostMapping("/batch")
    List<UserTagVo> batch(@RequestBody List<Long> uids);

    /** 按标签名取权重 &gt; 0 的用户 id；没有匹配返回空列表 */
    @PostMapping("/uids")
    List<Long> uidsByTag(@RequestBody TagUsersQueryVo query);
}
