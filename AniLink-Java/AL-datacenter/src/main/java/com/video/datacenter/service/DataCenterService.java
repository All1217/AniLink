package com.video.datacenter.service;

import com.video.datacenter.vo.DataResVo;
import com.video.model.entity.HotSearch;

import java.time.LocalDate;
import java.util.List;

/** 数据中心统计：播放/点赞趋势与首页热搜。 */
public interface DataCenterService {

    /** 总播放量趋势：本期/上期的总量与粉丝量序列 */
    DataResVo getTotalPlay(Long uid, LocalDate begin, LocalDate end);

    /** 总点赞趋势：本期/上期的点赞序列 */
    DataResVo getLike(Long uid, LocalDate begin, LocalDate end);

    /** 首页热搜列表（读算法引擎写入的共享 Redis 缓存） */
    List<HotSearch> getHotSearch();
}
