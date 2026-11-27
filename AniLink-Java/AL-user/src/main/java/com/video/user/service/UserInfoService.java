package com.video.user.service;

import com.video.api.user.dto.UserBriefVo;
import com.video.common.result.Result;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.vo.*;

import java.util.List;

public interface UserInfoService {
    UserInfo getById(Long id);

    /** 注册：返回新用户（含 uid/username/nickname），密码已做 bcrypt 哈希 */
    UserInfo insertOne(RegisterVo registerVo);

    UserStats getUserStatsByUid(Long uid);

    void updateNotice(UserStatsQueryVo queryVo);

    void updateDescription(UserInfoQueryVo queryVo);

    void updateUserStats(Long uid);

    void updateUserInfo(UserInfoQueryVo queryVo);

    UserVo getUserVoByUid(UserInfoQueryVo queryVo);

    void updateAvatar(UserInfoQueryVo queryVo);

    Result<List<UserInfo>> getSimilarUsers(List<Long> uids);

    /** 内部契约：批量取用户摘要，空列表直接返回空 */
    List<UserBriefVo> listBrief(List<Long> uids);
}
