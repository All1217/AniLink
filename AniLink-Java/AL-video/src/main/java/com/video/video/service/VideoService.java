package com.video.video.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.UserVideo;
import com.video.model.entity.Video;
import com.video.model.entity.VideoStats;
import com.video.video.vo.UserVideoQueryVo;
import com.video.video.vo.VideoQueryVo;
import com.video.video.vo.VideoVo;

/**
 * 视频元数据、数据统计与用户×视频交互（点赞状态、投币）。
 * 实时推荐（getRealTimeRecommend）归 al-datacenter，不再由本域提供。
 */
public interface VideoService {
    void insertOne(Video video);

    IPage<VideoVo> pageItem(IPage<VideoVo> page, VideoQueryVo queryVo);

    Video getById(Long vid);

    VideoStats getVideoStatsById(Long vid);

    UserVideo getInterActLike(UserVideoQueryVo userVideoQueryVo);

    UserVideo getInterActionCoin(UserVideoQueryVo userVideoQueryVo);

    UserVideo interActCoin(UserVideoQueryVo userVideoQueryVo);
}
