package com.video.video.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.result.Result;
import com.video.model.entity.UserVideo;
import com.video.model.entity.Video;
import com.video.model.entity.VideoStats;
import com.video.video.service.VideoService;
import com.video.video.vo.UserVideoQueryVo;
import com.video.video.vo.VideoQueryVo;
import com.video.video.vo.VideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 视频域对外接口（端口 8026，由网关 /main/video/** 转发）。
 * 实时推荐与播放进度上报不属于视频域：前者归 al-datacenter，后者归 al-playback。
 */
@Slf4j
@Tag(name = "视频与视频数据相关接口")
@RestController
@RequestMapping("/main/video")
public class VideoController {
    @Autowired
    private VideoService videoService;

    @Operation(summary = "条件分页查询视频")
    @GetMapping("pageVideo")
    public Result<IPage<VideoVo>> videoPageItem(@RequestParam long current, @RequestParam long size, VideoQueryVo queryVo) {
        IPage<VideoVo> page = new Page<>(current, size);
        IPage<VideoVo> result = videoService.pageItem(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "分页查询视频(免登录)")
    @GetMapping("/public/pageVideo")
    public Result<IPage<VideoVo>> homePageItem(@RequestParam long current, @RequestParam long size, VideoQueryVo queryVo) {
        log.info("queryVo: {}", queryVo);
        IPage<VideoVo> page = new Page<>(current, size);
        IPage<VideoVo> result = videoService.pageItem(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "根据ID查询视频信息（免登录）")
    @GetMapping("/public/getVideoById")
    public Result<Video> homeGetVideoById(@RequestParam Long vid) {
        Video res = videoService.getById(vid);
        return Result.ok(res);
    }

    @Operation(summary = "根据ID查询视频数据（免登录）")
    @GetMapping("/public/getVideoStatsById")
    public Result<VideoStats> homeGetVideoStatsById(@RequestParam Long vid) {
        VideoStats res = videoService.getVideoStatsById(vid);
        return Result.ok(res);
    }

    @Operation(summary = "查询点赞状态")
    @GetMapping("/getInterActLike")
    public Result<UserVideo> getInterActLike(UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = videoService.getInterActLike(userVideoQueryVo);
        return Result.ok(res);
    }

    @Operation(summary = "查询投币状态")
    @GetMapping("/getInterActionCoin")
    public Result<UserVideo> getInterActionCoin(UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = videoService.getInterActionCoin(userVideoQueryVo);
        return Result.ok(res);
    }

    @Operation(summary = "投币")
    @PostMapping("/interact/coin")
    public Result<UserVideo> interActCoin(@RequestBody UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = videoService.interActCoin(userVideoQueryVo);
        return Result.ok(res);
    }
}
