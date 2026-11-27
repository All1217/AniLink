package com.video.web.main.service.impl;

import com.video.web.main.mapper.LegacyUserStatsMapper;
import com.video.web.main.vo.UserStatsCountsVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户域迁移过渡适配层（Phase 3/4 删除）。
 *
 * <p>`follow`、`video`、`video_stats` 在 Phase 3 起逐步由 al-social / al-video 接管，
 * al-user 通过 /internal/v1 调这里完成统计聚合，避免跨域直接建表访问。
 * 默认收藏夹创建已迁到 al-social 的内部接口，这里只保留统计聚合。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserSupportService {

    private final LegacyUserStatsMapper userStatsMapper;

    /** 统计聚合：仍由 AL-web 查自己的表，返回给 al-user 写入 user_stats */
    public UserStatsCountsVo statsOf(Long uid) {
        UserStatsCountsVo vo = new UserStatsCountsVo();
        vo.setFansCount(userStatsMapper.calFansCount(uid));
        vo.setFollowCount(userStatsMapper.calFollowCount(uid));
        vo.setLikeCount(userStatsMapper.calLikeCount(uid));
        vo.setPlayCount(userStatsMapper.calPlayCount(uid));
        vo.setVideoCount(userStatsMapper.calVideoCount(uid));
        vo.setCollectCount(userStatsMapper.calCollectCount(uid));
        return vo;
    }
}
