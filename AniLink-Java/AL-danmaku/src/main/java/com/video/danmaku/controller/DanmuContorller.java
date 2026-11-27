package com.video.danmaku.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.video.common.result.Result;
import com.video.danmaku.service.DanmuService;
import com.video.model.entity.Danmu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 弹幕相关接口：只承接弹幕发送与公开列表。
 * 标签/推荐类接口（getSimilarUsers、filterUserByTags、getCommonTags、getTagRankList）
 * 在 Phase 4 迁入 al-datacenter 之前仍由 AL-web 提供，路由继续指向 webservice。
 */
@Tag(name = "弹幕相关接口")
@RestController
@RequestMapping("/main/danmu")
public class DanmuContorller {
    @Autowired
    private DanmuService danmuService;

    @Operation(summary = "发送弹幕")
    @PostMapping("/post")
    public Result postDanmu(@RequestBody Danmu danmu) {
        danmuService.insertOne(danmu);
        return Result.ok();
    }

    @Operation(summary = "查询弹幕列表")
    @GetMapping("/public/getDanmuList")
    public Result<List<Danmu>> getDanmuList(@RequestParam Long vid) {
        LambdaQueryWrapper<Danmu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Danmu::getVid, vid);
        queryWrapper.eq(Danmu::getStatus, 1);
        List<Danmu> list = danmuService.list(queryWrapper);
        return Result.ok(list);
    }
}
