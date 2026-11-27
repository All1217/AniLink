package com.video.web.main.controller;

import com.video.api.user.dto.UserBriefVo;
import com.video.common.result.Result;
import com.video.web.main.service.UserBriefService;
import com.video.web.main.service.VideoBriefService;
import com.video.web.main.vo.RecQueryVo;
import com.video.web.main.vo.VideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 实时推荐：Phase 2 暂留 AL-web（Phase 4 迁 al-datacenter）。
 * 网关用更具体的路由把该接口继续指向 webservice，其余 /main/video/** 已归属 al-video。
 */
@Slf4j
@Tag(name = "实时推荐（迁移期保留在 AL-web）")
@RestController
@RequestMapping("/main/video")
public class RecommendController {

    private final RestTemplate restTemplate;
    private final VideoBriefService videoBriefService;
    private final UserBriefService userBriefService;

    public RecommendController(RestTemplate restTemplate,
                               VideoBriefService videoBriefService,
                               UserBriefService userBriefService) {
        this.restTemplate = restTemplate;
        this.videoBriefService = videoBriefService;
        this.userBriefService = userBriefService;
    }

    @Operation(summary = "获取实时推荐列表")
    @GetMapping("/public/getRealTimeRecommend")
    public Result<List<VideoVo>> getRealTimeRecommend(RecQueryVo queryVo) {
        // 向 Flask 服务发送 GET 请求，返回的视频 ID 列表
        String url = "http://localhost:5000/recommendations/" + queryVo.getUid() + "?count=" + queryVo.getCount();
        List<?> raw = restTemplate.getForObject(url, List.class);
        log.info("推荐引擎返回 videoIds: {}", raw);
        if (raw == null || raw.isEmpty()) {
            return Result.ok(List.of());
        }
        // JSON 数字默认反序列化成 Integer，统一转 Long 再走 al-video 内部契约
        List<Long> videoIds = raw.stream()
                .filter(Objects::nonNull)
                .map(id -> ((Number) id).longValue())
                .toList();
        List<VideoVo> res = videoBriefService.legacyVideosInPlayOrder(videoIds);
        fillNicknames(res);
        log.info("实时推荐结果条数：{}", res.size());
        return Result.ok(res);
    }

    /** 作者昵称改由 al-user 批量补全，避免跨域 JOIN user */
    private void fillNicknames(List<VideoVo> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<Long, UserBriefVo> users = userBriefService.byUids(
                records.stream().map(VideoVo::getUid).toList());
        records.forEach(video -> {
            UserBriefVo user = users.get(video.getUid());
            video.setNickname(user == null ? null : user.getNickname());
        });
    }
}
