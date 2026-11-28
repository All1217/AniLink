package com.video.video.service;

import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;

import java.util.List;

/** 视频日汇总内部契约：按 UP 主与时间范围给数据中心提供每日播放/粉丝播放/点赞。 */
public interface VideoDailyStatsService {

    /** 返回 [begin, end] 内逐日汇总，缺失日期补 0；粉丝播放量按传入的粉丝 id 集合拆分 */
    List<VideoDailyStatsVo> dailyStats(VideoDailyStatsQueryVo query);
}
