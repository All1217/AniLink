package com.video.social.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.api.social.dto.UserFollowStatsVo;
import com.video.api.user.dto.UserBriefVo;
import com.video.model.entity.Follow;
import com.video.model.entity.UserInfo;
import com.video.social.mapper.FansMapper;
import com.video.social.service.FansService;
import com.video.social.service.UserBriefService;
import com.video.social.vo.FollowQueryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** 关注/粉丝服务实现：只操作 follow 表，用户资料由 al-user 批量补全。 */
@Service
@RequiredArgsConstructor
public class FansServiceImpl implements FansService {

    private final FansMapper fansMapper;
    private final UserBriefService userBriefService;

    @Override
    public void insertOne(Follow follow) {
        follow.setId(0L);
        follow.setCreatedAt(new Date());
        fansMapper.insertOne(follow);
    }

    @Override
    public void deleteById(Follow follow) {
        fansMapper.deleteById(follow);
    }

    @Override
    public Follow getFollow(Follow follow) {
        return fansMapper.getFollow(follow);
    }

    @Override
    public IPage<UserInfo> getFollowList(IPage<UserInfo> page, FollowQueryVo query) {
        IPage<UserInfo> result = fansMapper.getFollowList(page, query);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public IPage<UserInfo> getFansList(IPage<UserInfo> page, FollowQueryVo query) {
        IPage<UserInfo> result = fansMapper.getFansList(page, query);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public List<Long> listFansIds(Long uid) {
        if (uid == null) {
            return List.of();
        }
        return fansMapper.listFansIds(uid);
    }

    /** 关注统计：一次 SQL 取两个方向的 count；查询无结果时返回 0 而不是 null */
    @Override
    public UserFollowStatsVo userStats(Long uid) {
        UserFollowStatsVo stats = uid == null ? null : fansMapper.userStats(uid);
        if (stats != null) {
            return stats;
        }
        UserFollowStatsVo empty = new UserFollowStatsVo();
        empty.setFansCount(0L);
        empty.setFollowCount(0);
        return empty;
    }

    /** 用户资料改由 al-user 批量补全；role 是互相关注标记，由 SQL 计算后保持不动 */
    private void enrich(List<UserInfo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, UserBriefVo> users = userBriefService.byUids(records.stream().map(UserInfo::getUid).toList());
        records.forEach(user -> {
            UserBriefVo brief = users.get(user.getUid());
            if (brief == null) {
                return;
            }
            user.setNickname(brief.getNickname());
            user.setAvatar(brief.getAvatar());
            user.setDescription(brief.getDescription());
            user.setStatus(brief.getStatus());
        });
    }
}
