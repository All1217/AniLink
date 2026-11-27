package com.video.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.api.user.dto.UserBriefVo;
import com.video.api.video.dto.VideoBriefVo;
import com.video.api.video.dto.VideoStatsVo;
import com.video.model.entity.Favorite;
import com.video.model.entity.FavoriteVideo;
import com.video.model.entity.UserVideo;
import com.video.social.mapper.FavoriteMapper;
import com.video.social.mq.FavoriteEventPublisher;
import com.video.social.service.FavoriteService;
import com.video.social.service.UserBriefService;
import com.video.social.service.VideoBriefService;
import com.video.social.vo.BatchCollectVo;
import com.video.social.vo.BatchUpdateVo;
import com.video.social.vo.UserVideoQueryVo;
import com.video.social.vo.VideoFavoriteVo;
import com.video.social.vo.VideoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 收藏夹服务实现：只操作属主表 favorite / favorite_video。
 *
 * <p>收藏状态变化不再写 al-video 的 Redis 计数 key，改为提交后投递 {@code favorite.changed} 事件，
 * 由 al-video 幂等更新 user_video.collect 与 video_stats.collect。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final UserBriefService userBriefService;
    private final VideoBriefService videoBriefService;
    private final FavoriteEventPublisher eventPublisher;

    @Override
    public void insertOne(Favorite favorite) {
        favorite.setFid(0L);
        favoriteMapper.insert(favorite);
    }

    /** 收藏状态以 favorite/favorite_video 为唯一事实来源，不再读取 al-video 的 user_video */
    @Override
    public UserVideo getInterActionFavorite(UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = new UserVideo();
        int collected = favoriteMapper.countCollected(userVideoQueryVo.getUid(), userVideoQueryVo.getVid());
        res.setCollect(collected > 0 ? 1 : 0);
        return res;
    }

    @Override
    public IPage<Favorite> getFavoriteListPage(IPage<Favorite> page, Long uid) {
        return favoriteMapper.getFavoriteListPage(page, uid);
    }

    @Override
    public List<VideoVo> getVideoByFavor(long fid) {
        List<Long> videoIds = favoriteMapper.getVideoIds(fid);
        if (videoIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 视频信息改走 al-video 内部契约批量补全，不再 JOIN video/video_stats
        return videoBriefService.legacyVideosInPlayOrder(videoIds);
    }

    @Override
    public IPage<VideoFavoriteVo> getVideoFavoritePage(IPage<Favorite> page, long fid, String sortField) {
        // 前端只传 time；播放/收藏数排序需要跨服务数据，迁移期统一按收藏时间倒序
        if (sortField != null && !"time".equals(sortField)) {
            log.warn("收藏视频列表暂不支持按 {} 排序，已回退为收藏时间", sortField);
        }
        IPage<VideoFavoriteVo> result = favoriteMapper.getVideoFavoritePage(page, fid);
        enrichVideos(result.getRecords());
        return result;
    }

    /** 收藏页只返回收藏关系，视频字段由 al-video 批量补全后回填，保持旧响应结构 */
    private void enrichVideos(List<VideoFavoriteVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> vids = records.stream().map(VideoFavoriteVo::getVid).toList();
        Map<Long, VideoBriefVo> videos = videoBriefService.byVids(vids);
        Map<Long, VideoStatsVo> stats = videoBriefService.statsOf(vids);
        records.forEach(record -> {
            VideoBriefVo brief = videos.get(record.getVid());
            if (brief != null) {
                record.setUid(brief.getUid());
                record.setTitle(brief.getTitle());
                record.setDuration(brief.getDuration());
                record.setCoverUrl(brief.getCoverUrl());
                record.setCoverThumbUrl(brief.getCoverThumbUrl());
                record.setUploadDate(brief.getUploadDate());
                record.setPlay(brief.getPlay());
            }
            VideoStatsVo stat = stats.get(record.getVid());
            if (stat != null) {
                record.setPlay(stat.getPlay());
                record.setDanmu(stat.getDanmu());
                record.setGood(stat.getGood());
                record.setBad(stat.getBad());
                record.setCoin(stat.getCoin());
                record.setCollect(stat.getCollect());
                record.setShare(stat.getShare());
                record.setComment(stat.getComment());
            }
        });
        enrichNicknames(records);
    }

    /** 作者昵称改由 al-user 批量补全，避免跨域 JOIN user */
    private void enrichNicknames(List<VideoFavoriteVo> records) {
        Map<Long, UserBriefVo> users = userBriefService.byUids(records.stream().map(VideoFavoriteVo::getUid).toList());
        records.forEach(video -> {
            UserBriefVo user = users.get(video.getUid());
            video.setNickname(user == null ? null : user.getNickname());
        });
    }

    @Override
    public void update(Favorite favorite) {
        UpdateWrapper<Favorite> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("fid", favorite.getFid());
        Favorite newFavor = new Favorite();
        if (favorite.getIsDelete() != null) newFavor.setIsDelete(favorite.getIsDelete());
        if (favorite.getVisible() != null) newFavor.setVisible(favorite.getVisible());
        if (favorite.getDescription() != null) newFavor.setDescription(favorite.getDescription());
        if (favorite.getTitle() != null) newFavor.setTitle(favorite.getTitle());
        if (favorite.getCover() != null) newFavor.setCover(favorite.getCover());
        favoriteMapper.update(newFavor, updateWrapper);
    }

    @Override
    @Transactional
    public Integer batchMove(BatchUpdateVo q) {
        // 1.减旧收藏夹数量并删除旧收藏关系
        List<Long> vids = q.getVids();
        favoriteMapper.subCount(vids.size(), q.getPreFid());
        vids.forEach(e -> favoriteMapper.deleteFVById(q.getPreFid(), e));
        // 2.获取新收藏夹内视频ID列表，从全集消去重复元素（视频仍在收藏夹里，收藏状态不变，无需发事件）
        List<Long> targetFavorVids = favoriteMapper.getVideoIds(q.getNewFid());
        vids.removeAll(targetFavorVids);
        int res = 0;
        if (!vids.isEmpty()) {
            res = vids.size();
            // 3.更新新收藏夹视频数量并逐条插入收藏关系
            favoriteMapper.addCount(vids.size(), q.getNewFid());
            vids.forEach(e -> favoriteMapper.batchInsertSv(newFavoriteVideo(q.getNewFid(), e)));
        }
        return res;
    }

    @Override
    @Transactional
    public Integer batchCopy(BatchUpdateVo q) {
        List<Long> vids = q.getVids();
        List<Long> targetFavorVids = favoriteMapper.getVideoIds(q.getNewFid());
        vids.removeAll(targetFavorVids);
        int res = 0;
        if (!vids.isEmpty()) {
            res = vids.size();
            favoriteMapper.addCount(vids.size(), q.getNewFid());
            vids.forEach(e -> favoriteMapper.batchInsertSv(newFavoriteVideo(q.getNewFid(), e)));
        }
        return res;
    }

    @Override
    public List<Long> getFVByUidAndVid(long uid, long vid) {
        return favoriteMapper.getFVByUidAndVid(uid, vid);
    }

    @Override
    @Transactional
    public void collect(BatchCollectVo batchCollectVo) {
        Long vid = batchCollectVo.getVid();
        Long uid = batchCollectVo.getUid();
        List<Long> newList = batchCollectVo.getFids() == null ? new ArrayList<>() : new ArrayList<>(batchCollectVo.getFids());
        List<Long> oldList = favoriteMapper.getFVByUidAndVid(uid, vid);
        List<Long> toRemove = new ArrayList<>(oldList);
        toRemove.removeAll(newList);
        List<Long> toAdd = new ArrayList<>(newList);
        toAdd.removeAll(oldList);
        // 1.清理不再保留的收藏关系并回退收藏夹计数
        toRemove.forEach(id -> {
            favoriteMapper.deleteFVById(id, vid);
            favoriteMapper.subCount(1, id);
        });
        // 2.新增收藏关系并累加收藏夹计数
        toAdd.forEach(id -> {
            favoriteMapper.batchInsertSv(newFavoriteVideo(id, vid));
            favoriteMapper.addCount(1, id);
        });
        // 3.收藏状态发生翻转时投递事件；只在收藏夹之间移动不发事件，避免计数重复
        boolean wasCollected = !oldList.isEmpty();
        boolean nowCollected = !newList.isEmpty();
        if (wasCollected != nowCollected) {
            eventPublisher.publishAfterCommit(uid, vid, nowCollected ? 1 : 0, nowCollected ? 1 : -1);
        }
    }

    @Override
    @Transactional
    public void unCollect(BatchUpdateVo q) {
        // 1.减收藏夹数量并删除收藏关系
        List<Long> vids = q.getVids();
        if (vids == null || vids.isEmpty()) {
            return;
        }
        favoriteMapper.subCount(vids.size(), q.getPreFid());
        vids.forEach(e -> favoriteMapper.deleteFVById(q.getPreFid(), e));
        // 2.取消收藏入参没有 uid，用收藏夹反查属主
        Long uid = favoriteMapper.getFavoriteOwner(q.getPreFid());
        if (uid == null) {
            log.warn("取消收藏时未找到收藏夹属主，跳过事件投递：fid={}", q.getPreFid());
            return;
        }
        // 3.该用户已不在任何收藏夹保留该视频时，投递一次取消收藏事件
        vids.forEach(vid -> {
            if (favoriteMapper.countCollected(uid, vid) == 0) {
                eventPublisher.publishAfterCommit(uid, vid, 0, -1);
            }
        });
    }

    @Override
    @Transactional
    public boolean ensureDefaultFavorite(Long uid) {
        if (favoriteMapper.countDefaultFavorite(uid) > 0) {
            log.info("默认收藏夹已存在，跳过：uid={}", uid);
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setType(1);
        favorite.setTitle("默认收藏夹");
        favorite.setDescription("默认收藏夹");
        favorite.setUid(uid);
        favorite.setVisible(1);
        insertOne(favorite);
        return true;
    }

    /** 构造收藏关系记录（收藏时间为当前时间，未移除） */
    private FavoriteVideo newFavoriteVideo(Long fid, Long vid) {
        FavoriteVideo favoriteVideo = new FavoriteVideo();
        favoriteVideo.setFid(fid);
        favoriteVideo.setVid(vid);
        favoriteVideo.setTime(new Date());
        favoriteVideo.setIsRemove(0);
        return favoriteVideo;
    }
}
