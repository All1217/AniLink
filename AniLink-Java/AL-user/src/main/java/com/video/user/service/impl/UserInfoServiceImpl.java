package com.video.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.video.api.user.dto.UserBriefVo;
import com.video.common.exception.VideoException;
import com.video.common.result.Result;
import com.video.common.result.ResultCodeEnum;
import com.video.common.utils.CodeUtil;
import com.video.common.utils.PasswordUtil;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.adapter.LegacyUserSupportClient;
import com.video.user.mapper.UserInfoMapper;
import com.video.user.service.UserInfoService;
import com.video.user.util.AccountRules;
import com.video.user.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.video.common.constant.Constant.UNIQUE_UID_KEY;
import static com.video.common.result.ResultCodeEnum.APP_SERVER_ERROR;

@Service
@Slf4j
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    @Autowired
    private UserInfoMapper uiMapper;
    @Autowired
    private LegacyUserSupportClient legacySupport;
    @Autowired
    private StringRedisTemplate redisTemplate;
    private final BoundValueOperations<String, String> serialOps;

    public UserInfoServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(UNIQUE_UID_KEY);
    }

    @Override
    public UserInfo getById(Long id) {
        // 注意：按 uid 查，不能拿 uid 当 username（账号体系重做后 username 不再等于 uid）
        return uiMapper.getById(id);
    }

    /**
     * 注册业务逻辑：username 是用户自选的登录名（不再用系统生成的数字码），密码用 bcrypt 存储。
     */
    @Override
    @Transactional
    public UserInfo insertOne(RegisterVo registerVo) {
        // 1.规范化与规则校验（用户名/密码/昵称）
        String username = AccountRules.normalizeUsername(registerVo.getUsername());
        String nickname = AccountRules.normalizeNickname(registerVo.getNickname(), username);
        AccountRules.validateUsername(username);
        AccountRules.validatePassword(registerVo.getPassword());
        AccountRules.validateNickname(nickname);
        // 2.用户名占用检查（并发场景由唯一索引兜底）
        if (uiMapper.selectOneByUsername(username) != null) {
            throw new VideoException(ResultCodeEnum.USERNAME_TAKEN);
        }
        // 3.生成 uid（沿用可见码规则）
        Long sequence = serialOps.increment(1);
        if (sequence == null) {
            throw new VideoException(APP_SERVER_ERROR.getCode(), "服务器异常：生成自增序列号失败！");
        }
        Long uid = CodeUtil.geneVisibleCode(sequence, sequence);
        // 4.落库
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(uid);
        userInfo.setUsername(username);
        userInfo.setNickname(nickname);
        // 头像留空，由前端显示内置占位图；不再写入内网默认头像地址
        userInfo.setPassword(PasswordUtil.encode(registerVo.getPassword()));
        userInfo.setCreateDate(new Date());
        try {
            uiMapper.insert(userInfo);
        } catch (DuplicateKeyException e) {
            throw new VideoException(ResultCodeEnum.USERNAME_TAKEN);
        }
        // 5.初始化用户额外数据；默认收藏夹仍属 AL-web，过渡期通过内部接口创建
        UserStats userStats = new UserStats();
        userStats.setUid(uid);
        uiMapper.inserUserStats(userStats);
        createDefaultFavoriteBestEffort(uid);
        return userInfo;
    }

    /** 过渡期：favorite 表仍属 AL-web，创建失败只记日志，不影响注册 */
    private void createDefaultFavoriteBestEffort(Long uid) {
        try {
            legacySupport.ensureDefaultFavorite(uid);
        } catch (Exception e) {
            log.warn("创建默认收藏夹失败（过渡接口）：uid={}", uid, e);
        }
    }

    @Override
    public UserStats getUserStatsByUid(Long uid) {
        return uiMapper.getUserStatsByUid(uid);
    }

    @Override
    public void updateNotice(UserStatsQueryVo queryVo) {
        UserStats userStats = new UserStats();
        userStats.setUid(queryVo.getUid());
        userStats.setNotice(queryVo.getNotice());
        uiMapper.updateUserStats(userStats);
    }

    @Override
    public void updateDescription(UserInfoQueryVo queryVo) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(queryVo.getUid());
        userInfo.setDescription(queryVo.getDescription());
        uiMapper.updateUserInfo(userInfo);
    }

    @Override
    @Transactional
    public void updateUserStats(Long uid) {
        // 过渡期：follow / video / video_stats 仍属 AL-web，统计值从内部接口取
        UserStatsCountsVo counts = legacySupport.stats(uid).getData();
        if (counts == null) {
            return;
        }
        UserStats userStats = new UserStats();
        userStats.setUid(uid);
        userStats.setFansCount(counts.getFansCount());
        userStats.setFollowCount(counts.getFollowCount());
        userStats.setLikeCount(counts.getLikeCount());
        userStats.setPlayCount(counts.getPlayCount());
        userStats.setVideoCount(counts.getVideoCount());
        userStats.setCollectCount(counts.getCollectCount());
        uiMapper.updateUserStats(userStats);
    }

    @Override
    @Transactional
    public void updateUserInfo(UserInfoQueryVo queryVo) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(queryVo.getUid());
        userInfo.setAvatar(queryVo.getAvatar());
        userInfo.setNickname(queryVo.getNickname());
        userInfo.setGender(queryVo.getGender());
        userInfo.setDescription(queryVo.getDescription());
        uiMapper.updateUserInfo(userInfo);

        UserStats userStats = new UserStats();
        userStats.setUid(queryVo.getUid());
        userStats.setSchool(queryVo.getSchool());
        userStats.setBirthDate(queryVo.getBirthDate());
        log.info("impl userStats: {}", userStats);
        uiMapper.updateUserStats(userStats);
    }

    @Override
    public UserVo getUserVoByUid(UserInfoQueryVo queryVo) {
        return uiMapper.getUserVoByUid(queryVo);
    }

    @Override
    public void updateAvatar(UserInfoQueryVo queryVo) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUid(queryVo.getUid());
        userInfo.setAvatar(queryVo.getAvatar());
        uiMapper.updateUserInfo(userInfo);
    }

    @Override
    public Result<List<UserInfo>> getSimilarUsers(List<Long> uids) {
        List<UserInfo> res = lambdaQuery()
                .in(UserInfo::getUid, uids)
                .list();
        Collections.reverse(res);
        return Result.ok(res);
    }

    @Override
    public List<UserBriefVo> listBrief(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(UserInfo::getUid, uids)
                .select(UserInfo::getUid, UserInfo::getNickname, UserInfo::getAvatar,
                        UserInfo::getDescription, UserInfo::getStatus)
                .list()
                .stream()
                .map(user -> {
                    UserBriefVo vo = new UserBriefVo();
                    vo.setUid(user.getUid());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    vo.setDescription(user.getDescription());
                    vo.setStatus(user.getStatus());
                    return vo;
                })
                .toList();
    }
}
