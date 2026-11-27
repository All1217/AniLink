package com.video.user.controller.internal;

import com.video.api.user.dto.UserBriefVo;
import com.video.model.entity.UserStats;
import com.video.user.service.UserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用户域内部契约：批量查询、空列表、不存在的 id。 */
class InternalUserControllerTest {

    private UserInfoService userInfoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userInfoService = mock(UserInfoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalUserController(userInfoService, mock(com.video.user.service.LoginService.class))).build();
    }

    @Test
    void batchReturnsUsers() throws Exception {
        UserBriefVo vo = new UserBriefVo();
        vo.setUid(1L);
        vo.setNickname("测试用户");
        vo.setAvatar("http://localhost/avatar/a.webp");
        when(userInfoService.listBrief(List.of(1L))).thenReturn(List.of(vo));

        mockMvc.perform(post("/internal/v1/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uid").value(1))
                .andExpect(jsonPath("$[0].nickname").value("测试用户"));
    }

    @Test
    void emptyIdsReturnEmptyArray() throws Exception {
        when(userInfoService.listBrief(anyList())).thenReturn(List.of());

        mockMvc.perform(post("/internal/v1/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void missingIdsReturnEmptyArray() throws Exception {
        when(userInfoService.listBrief(List.of(999L))).thenReturn(List.of());

        mockMvc.perform(post("/internal/v1/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[999]"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void statsOfMissingUserIsEmpty() throws Exception {
        when(userInfoService.getUserStatsByUid(999L)).thenReturn(null);

        mockMvc.perform(get("/internal/v1/users/999/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void statsMapsCounts() throws Exception {
        UserStats stats = new UserStats();
        stats.setUid(7L);
        stats.setFansCount(11L);
        stats.setFollowCount(3);
        stats.setLikeCount(22L);
        stats.setPlayCount(33L);
        stats.setVideoCount(4);
        stats.setCollectCount(5);
        when(userInfoService.getUserStatsByUid(7L)).thenReturn(stats);

        mockMvc.perform(get("/internal/v1/users/7/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fansCount").value(11))
                .andExpect(jsonPath("$.videoCount").value(4));
    }
}
