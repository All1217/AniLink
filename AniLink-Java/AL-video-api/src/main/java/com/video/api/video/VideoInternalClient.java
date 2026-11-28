package com.video.api.video;

import com.video.api.video.dto.UserVideoStatsVo;
import com.video.api.video.dto.VideoBriefVo;
import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.api.video.dto.VideoStatsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 视频域内部契约：只给其他服务调用，网关不暴露 /internal/**。
 * 调用方用批量接口 + 本地短 TTL 缓存，禁止循环逐条调用。
 */
@FeignClient(name = "videoservice", contextId = "videoInternalClient",
        url = "${al.video.service-url:}", path = "/internal/v1/videos")
public interface VideoInternalClient {

    /** 批量取视频摘要；不存在的 id 不出现在结果里 */
    @PostMapping("/batch")
    List<VideoBriefVo> batch(@RequestBody List<Long> vids);

    /** 取单个视频统计；视频不存在返回 null */
    @GetMapping("/{vid}/stats")
    VideoStatsVo stats(@PathVariable("vid") Long vid);

    /** 取 UP 主视频统计汇总（获赞/播放/投稿/收藏）；没有稿件时各计数为 0 */
    @GetMapping("/users/{uid}/stats")
    UserVideoStatsVo userStats(@PathVariable("uid") Long uid);

    /**
     * 按 UP 主与时间范围取日汇总：返回 [begin, end] 内每天的播放/粉丝播放/点赞，缺失日期补 0。
     * 粉丝播放量依赖调用方传入的粉丝 id 集合，视频域不跨域读取 follow 表。
     */
    @PostMapping("/daily-stats")
    List<VideoDailyStatsVo> dailyStats(@RequestBody VideoDailyStatsQueryVo query);
}
