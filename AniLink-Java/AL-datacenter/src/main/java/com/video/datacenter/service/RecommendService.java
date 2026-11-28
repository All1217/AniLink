package com.video.datacenter.service;

import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.VideoVo;

import java.util.List;

/** 实时推荐：Flask 推荐视频 id → al-video 批量补全视频与统计。 */
public interface RecommendService {

    /** 获取实时推荐列表；引擎不可用时返回空列表 */
    List<VideoVo> getRealTimeRecommend(RecQueryVo queryVo);
}
