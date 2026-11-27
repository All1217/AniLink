package com.video.video.service;

import com.video.model.dto.social.FavoriteChangedDTO;
import com.video.video.mapper.UserVideoMapper;
import com.video.video.mapper.VideoStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 收藏事件落库：更新 user_video.collect 与 video_stats.collect。
 *
 * <p>幂等由监听器的 {@code eventId} Redis 去重保证，这里只做本域两张表的原子更新。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteCollectService {

    private final UserVideoMapper userVideoMapper;
    private final VideoStatsMapper videoStatsMapper;

    /** 消费一条收藏变更事件：覆盖用户×视频收藏标记，并按增量更新视频收藏数 */
    @Transactional
    public void apply(FavoriteChangedDTO event) {
        // 1.归一化事件字段，避免异常消息污染计数
        int collected = event.getCollected() != null && event.getCollected() > 0 ? 1 : 0;
        int delta = event.getDelta() == null ? 0 : event.getDelta();
        // 2.更新用户×视频收藏标记（收藏时记录收藏时间，取消收藏保留原时间）
        userVideoMapper.upsertCollect(event.getUid(), event.getVid(), collected, collected == 1 ? new Date() : null);
        // 3.按增量更新视频收藏总数，视频统计行缺失时记日志不阻断
        int rows = videoStatsMapper.addCollect(event.getVid(), delta);
        if (rows == 0) {
            log.warn("收藏事件未找到视频统计行，已跳过计数更新：vid={}, eventId={}", event.getVid(), event.getEventId());
        }
    }
}
