package com.video.datacenter.controller;

import com.video.common.result.Result;
import com.video.datacenter.service.RecommendService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.VideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 实时推荐接口：Phase 4 从 AL-web 迁入数据中心。
 * 网关对 /main/video/public/getRealTimeRecommend 用更具体的路由指向 datacenterservice。
 */
@Tag(name = "实时推荐")
@RestController
@RequestMapping("/main/video")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @Operation(summary = "获取实时推荐列表")
    @GetMapping("/public/getRealTimeRecommend")
    public Result<List<VideoVo>> getRealTimeRecommend(RecQueryVo queryVo) {
        return Result.ok(recommendService.getRealTimeRecommend(queryVo));
    }
}
