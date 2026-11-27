package com.video.social.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.Follow;
import com.video.model.entity.UserInfo;
import com.video.social.vo.FollowQueryVo;

/** 关注/粉丝域服务。 */
public interface FansService {

    /** 关注 */
    void insertOne(Follow follow);

    /** 取消关注 */
    void deleteById(Follow follow);

    /** 查询关注状态 */
    Follow getFollow(Follow follow);

    /** 分页查关注列表 */
    IPage<UserInfo> getFollowList(IPage<UserInfo> page, FollowQueryVo query);

    /** 分页查粉丝列表 */
    IPage<UserInfo> getFansList(IPage<UserInfo> page, FollowQueryVo query);
}
