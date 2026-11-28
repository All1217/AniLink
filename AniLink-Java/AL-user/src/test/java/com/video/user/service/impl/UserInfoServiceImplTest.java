package com.video.user.service.impl;

import com.video.api.social.SocialInternalClient;
import com.video.api.video.VideoInternalClient;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.adapter.SocialFavoriteClient;
import com.video.user.mapper.UserInfoMapper;
import com.video.user.vo.UserInfoQueryVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 用户资料更新单测：user 表与 user_stats 表分别按「至少一个字段」动态更新。
 * 回归缺陷 D1：只改昵称时曾无条件调用 updateUserStats，生成空 SET 的非法 SQL，
 * 导致 POST /main/user/updateUserInfo 必然返回 code 201。
 */
@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private UserInfoMapper uiMapper;
    @Mock
    private SocialInternalClient socialInternalClient;
    @Mock
    private VideoInternalClient videoInternalClient;
    @Mock
    private SocialFavoriteClient socialFavoriteClient;
    @Mock
    private StringRedisTemplate redisTemplate;

    private UserInfoServiceImpl userInfoService;

    @BeforeEach
    void setUp() {
        // 构造器只接 StringRedisTemplate，其余 @Autowired 字段用反射补齐（避免 Spring 上下文）
        userInfoService = new UserInfoServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(userInfoService, "uiMapper", uiMapper);
        ReflectionTestUtils.setField(userInfoService, "socialInternalClient", socialInternalClient);
        ReflectionTestUtils.setField(userInfoService, "videoInternalClient", videoInternalClient);
        ReflectionTestUtils.setField(userInfoService, "socialFavoriteClient", socialFavoriteClient);
    }

    @Test
    @DisplayName("仅改昵称：只更新 user 表，不触发 user_stats 更新（避免空 SET 非法 SQL）")
    void updateOnlyNicknameSkipsStatsUpdate() {
        UserInfoQueryVo queryVo = new UserInfoQueryVo();
        queryVo.setUid(100L);
        queryVo.setNickname("新昵称");

        userInfoService.updateUserInfo(queryVo);

        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(uiMapper).updateUserInfo(userCaptor.capture());
        assertEquals(100L, userCaptor.getValue().getUid());
        assertEquals("新昵称", userCaptor.getValue().getNickname());
        // 关键断言：只改用户表字段时绝不能调用 stats 更新
        verify(uiMapper, never()).updateUserStats(any());
    }

    @Test
    @DisplayName("改学校/生日：只更新 user_stats 表，不生成空的 user 表 SET")
    void updateSchoolAndBirthDateUpdatesStatsOnly() {
        Date birthDate = new Date(0L);
        UserInfoQueryVo queryVo = new UserInfoQueryVo();
        queryVo.setUid(100L);
        queryVo.setSchool("AniLink 大学");
        queryVo.setBirthDate(birthDate);

        userInfoService.updateUserInfo(queryVo);

        ArgumentCaptor<UserStats> statsCaptor = ArgumentCaptor.forClass(UserStats.class);
        verify(uiMapper).updateUserStats(statsCaptor.capture());
        assertEquals(100L, statsCaptor.getValue().getUid());
        assertEquals("AniLink 大学", statsCaptor.getValue().getSchool());
        assertEquals(birthDate, statsCaptor.getValue().getBirthDate());
        // 反向保护：没有 user 表字段时不执行 user 表更新，避免 `update user where uid = ?`
        verify(uiMapper, never()).updateUserInfo(any());
    }

    @Test
    @DisplayName("昵称与学校同时修改：两张表各更新一次")
    void updateNicknameAndSchoolUpdatesBothTables() {
        UserInfoQueryVo queryVo = new UserInfoQueryVo();
        queryVo.setUid(100L);
        queryVo.setNickname("新昵称");
        queryVo.setSchool("AniLink 大学");

        userInfoService.updateUserInfo(queryVo);

        verify(uiMapper).updateUserInfo(any());
        verify(uiMapper).updateUserStats(any());
    }
}
