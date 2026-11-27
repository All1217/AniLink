package com.video.web.main.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.common.constant.Constant;
import com.video.common.exception.VideoException;
import com.video.common.result.ResultCodeEnum;
import com.video.model.entity.UserVideo;
import com.video.model.entity.Video;
import com.video.model.entity.VideoStats;
import com.video.web.main.mapper.VideoMapper;
import com.video.web.main.mapper.VideoStatsMapper;
import com.video.web.main.service.VideoService;
import com.video.web.main.vo.UserVideoQueryVo;
import com.video.web.main.vo.VideoQueryVo;
import com.video.web.main.vo.VideoVo;
import com.video.web.main.util.ImageUrls;
import com.video.api.user.dto.UserBriefVo;
import com.video.web.main.service.UserBriefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final VideoMapper videoMapper;
    private final VideoStatsMapper videoStatsMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserBriefService userBriefService;

    @Override
    @Transactional
    public void insertOne(Video video) {
        video.setUploadDate(new Date());
        // 封面存的是 960w 主图，480w 缩略图地址按命名约定推导后一起落库
        video.setCoverThumbUrl(ImageUrls.thumbOf(video.getCoverUrl()));
        videoMapper.insertOne(video);
        VideoStats videoStats = new VideoStats();
        videoStats.setVid(video.getVid());
        videoStats.setPlay(0L);
        videoStats.setDanmu(0L);
        videoStats.setGood(0L);
        videoStats.setBad(0L);
        videoStats.setCoin(0L);
        videoStats.setCollect(0L);
        videoStats.setShare(0L);
        videoStats.setComment(0L);
        videoStatsMapper.insertOneStats(videoStats);
    }

    @Override
    public IPage<VideoVo> pageItem(IPage<VideoVo> page, VideoQueryVo queryVo) {
        IPage<VideoVo> result = videoMapper.pageItem(page, queryVo);
        enrichNicknames(result.getRecords());
        return result;
    }

    @Override
    public Video getById(Long vid) {
        return videoMapper.getById(vid);
    }

    @Override
    public VideoStats getVideoStatsById(Long vid) {
        return videoStatsMapper.getVideoStatsById(vid);
    }

    @Override
    @Transactional
    public List<VideoVo> getRealTimeRecommend(List<Long> videoIds) {
        List<VideoVo> result = videoMapper.batchSelect(videoIds);
        enrichNicknames(result);
        return result;
    }

    /** 用户昵称改由 al-user 批量补全，避免跨域 JOIN user */
    private void enrichNicknames(List<VideoVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, UserBriefVo> users = userBriefService.byUids(
                records.stream().map(VideoVo::getUid).toList());
        records.forEach(video -> {
            UserBriefVo user = users.get(video.getUid());
            video.setNickname(user == null ? null : user.getNickname());
        });
    }

    @Override
    @Transactional
    public UserVideo getInterActLike(UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = new UserVideo();
        String videoKey = Constant.MAIN_VIDEO_LIKE_PREFIX + userVideoQueryVo.getVid();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(videoKey))) {
            Integer tCount = (Integer) redisTemplate.opsForHash().get(videoKey, userVideoQueryVo.getUid());
            if (tCount != null) res.setLove(tCount);
            else {
                UserVideo doc = videoStatsMapper.getInterActLike(userVideoQueryVo);
                res.setLove(doc == null ? 0 : doc.getLove());
            }
        } else {
            UserVideo doc = videoStatsMapper.getInterActLike(userVideoQueryVo);
            res.setLove(doc == null ? 0 : doc.getLove());
        }
        return res;
    }

    @Override
    @Transactional
    public UserVideo getInterActionCoin(UserVideoQueryVo userVideoQueryVo) {
        UserVideo res = new UserVideo();
        String videoKey = Constant.MAIN_VIDEO_COIN_PREFIX + userVideoQueryVo.getVid();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(videoKey))) {
            Integer tCount = (Integer) redisTemplate.opsForHash().get(videoKey, userVideoQueryVo.getUid());
            if (tCount != null) res.setCoin(tCount);
            else {
                UserVideo temp = videoStatsMapper.getInterActionCoin(userVideoQueryVo);
                res.setCoin(temp == null ? 0 : temp.getCoin());
            }
        } else {
            UserVideo temp = videoStatsMapper.getInterActionCoin(userVideoQueryVo);
            res.setCoin(temp == null ? 0 : temp.getCoin());
        }
        return res;
    }

    @Override
    public UserVideo interActCoin(UserVideoQueryVo userVideoQueryVo) {
        // actionType 是交互枚举（2=投1枚，3=投2枚），不是币数，先换算成 1/2
        Integer actionType = userVideoQueryVo.getActionType();
        int requested = actionType == null ? 0 : actionType - 1;
        if (requested < 1) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "投币数量不合法");
        }
        int target = Math.min(requested, 2);
        String videoKey = Constant.MAIN_VIDEO_COIN_PREFIX + userVideoQueryVo.getVid();
        // 直接写入本次币数：旧逻辑把枚举值当币数写入，历史值 2 可能代表 1 枚，
        // 不能再与旧值取 max，否则投 1 枚会被抬成 2。
        redisTemplate.opsForHash().put(videoKey, userVideoQueryVo.getUid(), target);
        UserVideo res = new UserVideo();
        res.setCoin(target);
        return res;
    }
}
