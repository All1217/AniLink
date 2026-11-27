package com.video.web.main.service.impl;

import com.video.model.entity.Favorite;
import com.video.web.main.mapper.FavoriteMapper;
import com.video.web.main.mapper.LegacyUserStatsMapper;
import com.video.web.main.service.FavoriteService;
import com.video.web.main.vo.UserStatsCountsVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户域迁移过渡适配层（Phase 3/4 删除）。
 *
 * <p>`follow`、`video`、`video_stats`、`favorite` 在 Phase 1 仍属 AL-web，
 * al-user 通过 /internal/v1 调这里完成统计聚合与默认收藏夹创建，避免跨域直接建表访问。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserSupportService {

    private final LegacyUserStatsMapper userStatsMapper;
    private final FavoriteMapper favoriteMapper;
    private final FavoriteService favoriteService;

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

    /** 创建默认收藏夹：已有则跳过，保证注册重试幂等 */
    @Transactional
    public boolean ensureDefaultFavorite(Long uid) {
        if (favoriteMapper.countDefaultFavorite(uid) > 0) {
            log.info("默认收藏夹已存在，跳过：uid={}", uid);
            return false;
        }
        Favorite tf = new Favorite();
        tf.setType(1);
        tf.setTitle("默认收藏夹");
        tf.setDescription("默认收藏夹");
        tf.setUid(uid);
        tf.setVisible(1);
        favoriteService.insertOne(tf);
        return true;
    }
}
