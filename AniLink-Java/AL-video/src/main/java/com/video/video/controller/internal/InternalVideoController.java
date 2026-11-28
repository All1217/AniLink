package com.video.video.controller.internal;

import com.video.api.video.dto.UserVideoStatsVo;
import com.video.api.video.dto.VideoBriefVo;
import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.api.video.dto.VideoStatsVo;
import com.video.model.entity.VideoStats;
import com.video.video.mapper.VideoMapper;
import com.video.video.mapper.VideoStatsMapper;
import com.video.video.service.VideoDailyStatsService;
import com.video.video.vo.VideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 视频域内部契约实现，只给其他服务调用（网关不暴露 /internal/**）。
 * 返回裸 DTO 而不是 Result，便于 Feign 直接反序列化。
 */
@Tag(name = "内部-视频域")
@RestController
@RequestMapping("/internal/v1/videos")
public class InternalVideoController {

    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final VideoDailyStatsService videoDailyStatsService;

    public InternalVideoController(VideoMapper videoMapper, VideoStatsMapper videoStatsMapper,
                                   VideoDailyStatsService videoDailyStatsService) {
        this.videoMapper = videoMapper;
        this.videoStatsMapper = videoStatsMapper;
        this.videoDailyStatsService = videoDailyStatsService;
    }

    /** 批量取视频摘要：沿用视频列表的公开可见/审核通过过滤，不存在的 id 不出现在结果里 */
    @Operation(summary = "批量取视频摘要")
    @PostMapping("/batch")
    public List<VideoBriefVo> batch(@RequestBody List<Long> vids) {
        if (vids == null || vids.isEmpty()) {
            return List.of();
        }
        return videoMapper.batchSelect(vids).stream().map(this::toBrief).toList();
    }

    /** 取单个视频统计：视频不存在（无统计行）返回 null */
    @Operation(summary = "取视频统计（不存在返回 null）")
    @GetMapping("/{vid}/stats")
    public VideoStatsVo stats(@PathVariable("vid") Long vid) {
        VideoStats stats = videoStatsMapper.getVideoStatsById(vid);
        if (stats == null) {
            return null;
        }
        VideoStatsVo vo = new VideoStatsVo();
        vo.setVid(stats.getVid());
        vo.setPlay(stats.getPlay());
        vo.setDanmu(stats.getDanmu());
        vo.setGood(stats.getGood());
        vo.setBad(stats.getBad());
        vo.setCoin(stats.getCoin());
        vo.setCollect(stats.getCollect());
        vo.setShare(stats.getShare());
        vo.setComment(stats.getComment());
        return vo;
    }

    /** 按 UP 主与时间范围取日汇总：数据中心画播放/点赞趋势用，缺失日期补 0 */
    @Operation(summary = "按 UP 主与时间范围取日汇总")
    @PostMapping("/daily-stats")
    public List<VideoDailyStatsVo> dailyStats(@RequestBody VideoDailyStatsQueryVo query) {
        return videoDailyStatsService.dailyStats(query);
    }

    /** 取 UP 主视频统计汇总：获赞/播放/投稿/收藏，没有稿件时各项为 0 */
    @Operation(summary = "取 UP 主视频统计汇总")
    @GetMapping("/users/{uid}/stats")
    public UserVideoStatsVo userStats(@PathVariable("uid") Long uid) {
        UserVideoStatsVo stats = videoMapper.userVideoStats(uid);
        return stats == null ? emptyUserStats() : stats;
    }

    /** 聚合查询无稿件时也应返回 0，而不是 null，方便调用方直接落库 */
    private UserVideoStatsVo emptyUserStats() {
        UserVideoStatsVo stats = new UserVideoStatsVo();
        stats.setLikeCount(0L);
        stats.setPlayCount(0L);
        stats.setVideoCount(0);
        stats.setCollectCount(0);
        return stats;
    }

    /** 列表查询结果转跨服务摘要，只暴露收藏/历史/评论补全所需字段 */
    private VideoBriefVo toBrief(VideoVo video) {
        VideoBriefVo vo = new VideoBriefVo();
        vo.setVid(video.getVid());
        vo.setUid(video.getUid());
        vo.setTitle(video.getTitle());
        vo.setDuration(video.getDuration());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setCoverThumbUrl(video.getCoverThumbUrl());
        vo.setUploadDate(video.getUploadDate());
        vo.setPlay(video.getPlay());
        return vo;
    }
}
