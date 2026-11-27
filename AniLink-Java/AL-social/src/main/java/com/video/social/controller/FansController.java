package com.video.social.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.result.Result;
import com.video.model.entity.Follow;
import com.video.model.entity.UserInfo;
import com.video.social.service.FansService;
import com.video.social.vo.FollowQueryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 粉丝管理相关接口（路径对外保持 /main/fans/** 不变）。 */
@Tag(name = "粉丝管理相关接口")
@RestController
@RequestMapping("/main/fans")
@RequiredArgsConstructor
public class FansController {

    private final FansService fansService;

    @Operation(summary = "关注")
    @PostMapping("/follow")
    public Result follow(@RequestBody Follow follow) {
        fansService.insertOne(follow);
        return Result.ok();
    }

    @Operation(summary = "查询关注状态")
    @GetMapping("/get/follow")
    public Result<Follow> getFollow(Follow follow) {
        Follow res = fansService.getFollow(follow);
        return Result.ok(res);
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/unfollow")
    public Result unfollow(@RequestBody Follow follow) {
        fansService.deleteById(follow);
        return Result.ok();
    }

    @Operation(summary = "查询粉丝列表")
    @GetMapping("/get/fans")
    public Result<Follow> follow(@RequestParam Long uid) {
        Follow res = new Follow();
        return Result.ok(res);
    }

    @Operation(summary = "查询关注列表")
    @GetMapping("/get/followList")
    public Result<IPage<UserInfo>> getFollowList(FollowQueryVo query) {
        IPage<UserInfo> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<UserInfo> res = fansService.getFollowList(page, query);
        return Result.ok(res);
    }

    @Operation(summary = "查询粉丝列表")
    @GetMapping("/get/fansList")
    public Result<IPage<UserInfo>> getFansList(FollowQueryVo query) {
        IPage<UserInfo> page = new Page<>(query.getCurrent(), query.getSize());
        IPage<UserInfo> res = fansService.getFansList(page, query);
        return Result.ok(res);
    }
}
