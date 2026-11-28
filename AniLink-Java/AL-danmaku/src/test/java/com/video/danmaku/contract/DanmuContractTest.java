package com.video.danmaku.contract;

import com.video.danmaku.controller.DanmuContorller;
import com.video.danmaku.service.DanmuService;
import com.video.model.entity.Danmu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 契约补测：弹幕公开列表与发送接口的对外 JSON 契约。
 * MockMvc standalone，不依赖 Nacos/MySQL/Netty，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class DanmuContractTest {

    @Mock
    private DanmuService danmuService;

    @InjectMocks
    private DanmuContorller danmuController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(danmuController).build();
    }

    @Test
    @DisplayName("弹幕公开列表返回 code=200 + data 数组")
    void publicDanmuListKeepsArrayContract() throws Exception {
        Danmu danmu = new Danmu();
        danmu.setId(1L);
        danmu.setVid(1001L);
        danmu.setUid(7L);
        danmu.setContent("前方高能");
        danmu.setStatus(1);
        when(danmuService.list(any())).thenReturn(List.of(danmu));

        mockMvc.perform(get("/main/danmu/public/getDanmuList").param("vid", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data[0].vid").value(1001))
                .andExpect(jsonPath("$.data[0].content").value("前方高能"));
    }

    @Test
    @DisplayName("发送弹幕返回 code=200 且不返回业务数据")
    void postDanmuKeepsOkContract() throws Exception {
        doNothing().when(danmuService).insertOne(any(Danmu.class));

        mockMvc.perform(post("/main/danmu/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vid\":1001,\"uid\":7,\"content\":\"契约测试弹幕\",\"timePoint\":12.5,"
                                + "\"status\":1,\"createDate\":\"2026-11-28 22:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"));
    }
}
