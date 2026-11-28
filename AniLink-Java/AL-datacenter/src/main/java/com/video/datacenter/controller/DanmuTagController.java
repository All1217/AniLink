package com.video.datacenter.controller;

import com.video.common.result.Result;
import com.video.datacenter.service.TagService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.TagRankListVO;
import com.video.model.entity.Danmu;
import com.video.model.entity.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 弹幕域的推荐/标签接口：Phase 4 从 AL-web 迁入数据中心。
 * 网关对这 4 条具体路径用更高优先级路由指向 datacenterservice，其余 /main/danmu/** 仍归 al-danmaku。
 */
@Tag(name = "弹幕标签/推荐接口")
@RestController
@RequestMapping("/main/danmu")
public class DanmuTagController {

    private final TagService tagService;

    public DanmuTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "根据弹幕获取相似用户")
    @GetMapping("/getSimilarUsers")
    public Result<List<UserInfo>> getSimilarUsers(RecQueryVo queryVo) {
        return tagService.getSimilarUsers(queryVo);
    }

    @Operation(summary = "根据指定标签过滤弹幕")
    @PostMapping("/filterUserByTags")
    public Result<List<Danmu>> filterUserByTags(@RequestBody RecQueryVo queryVo) {
        return tagService.filterUserByTags(queryVo);
    }

    // 在这个接口中，uid和vid实际上均表示用户ID
    @Operation(summary = "查找两个用户的公共标签")
    @GetMapping("/getCommonTags")
    public Result<List<String>> getCommonTags(RecQueryVo queryVo) {
        return tagService.getCommonTags(queryVo);
    }

    @Operation(summary = "获取出现频率前八的标签")
    @GetMapping("/getTagRankList")
    public Result<List<TagRankListVO>> getTagRankList(@RequestParam Long vid) {
        return tagService.getTagRankList(vid);
    }
}
