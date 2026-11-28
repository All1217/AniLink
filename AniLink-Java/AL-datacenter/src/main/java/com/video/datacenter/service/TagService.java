package com.video.datacenter.service;

import com.video.common.result.Result;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.TagRankListVO;
import com.video.model.entity.Danmu;
import com.video.model.entity.UserInfo;

import java.util.List;

/** 推荐/标签类接口：相似用户、标签过滤弹幕、共同标签与标签排行。 */
public interface TagService {

    /** 根据弹幕获取相似用户：Flask 给 uid 列表，用户资料走 al-user 批量补全 */
    Result<List<UserInfo>> getSimilarUsers(RecQueryVo queryVo);

    /** 按用户标签过滤某视频的弹幕：标签来自 al-user，弹幕明细来自 al-danmaku */
    Result<List<Danmu>> filterUserByTags(RecQueryVo queryVo);

    /** 两个用户的共同标签：user_tag 归 al-user，直接走内部契约 */
    Result<List<String>> getCommonTags(RecQueryVo queryVo);

    /** 某视频出现频率前八的标签：用户标签 × 弹幕明细在内存聚合 */
    Result<List<TagRankListVO>> getTagRankList(Long vid);
}
