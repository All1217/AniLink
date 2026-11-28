package com.video.user.service;

import com.video.api.user.dto.UserTagVo;

import java.util.List;

/**
 * 用户标签查询服务：user_tag 自 Phase 4 起归 al-user。
 * 只做单表查询，标签/弹幕的聚合统计由数据中心 BFF 负责。
 */
public interface UserTagService {

    /** 取两个用户的共同标签名 */
    List<String> commonTags(Long uid, Long otherUid);

    /** 批量取用户标签行，空入参返回空 */
    List<UserTagVo> batchTags(List<Long> uids);

    /** 按标签名取权重 > 0 的用户 id */
    List<Long> uidsByTag(String tagName);
}
