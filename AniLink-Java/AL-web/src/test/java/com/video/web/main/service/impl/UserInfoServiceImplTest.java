package com.video.web.main.service.impl;

import com.video.common.constant.Constant;
import com.video.model.entity.UserInfo;
import com.video.web.main.mapper.UserInfoMapper;
import com.video.web.main.service.FavoriteService;
import com.video.web.main.vo.RegisterVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 注册流程：不再写入内网默认头像。 */
@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private UserInfoMapper uiMapper;
    @Mock
    private FavoriteService favoriteService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private BoundValueOperations<String, String> serialOps;

    private UserInfoServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.boundValueOps(Constant.UNIQUE_UID_KEY)).thenReturn(serialOps);
        service = new UserInfoServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(service, "uiMapper", uiMapper);
        ReflectionTestUtils.setField(service, "favoriteService", favoriteService);
    }

    @Test
    void registerLeavesAvatarEmpty() {
        when(uiMapper.selectOneByUsername("tester01")).thenReturn(null);
        when(serialOps.increment(1)).thenReturn(1L);

        RegisterVo vo = new RegisterVo();
        vo.setUsername("tester01");
        vo.setPassword("Str0ngPass");
        vo.setNickname("测试用户");
        service.insertOne(vo);

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(uiMapper).insert(captor.capture());
        assertNull(captor.getValue().getAvatar(), "注册不应再写入默认头像地址");
        assertNotNull(captor.getValue().getPassword(), "密码应已加密落库");
        verify(favoriteService).insertOne(any());
    }
}
