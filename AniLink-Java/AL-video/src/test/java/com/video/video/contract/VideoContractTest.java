package com.video.video.contract;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.video.controller.VideoController;
import com.video.video.service.VideoService;
import com.video.video.vo.VideoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 视频分页接口契约快照：只校验网关对外 JSON 结构（code/message/data.records）。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class VideoContractTest {

    @Mock
    private VideoService videoService;

    @InjectMocks
    private VideoController videoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(videoController).build();
    }

    @Test
    void pageVideoKeepsPagedRecordsContract() throws Exception {
        // 构造一条视频数据，断言分页壳与记录关键字段（coverThumbUrl 为列表缩略图契约）
        VideoVo vo = new VideoVo();
        vo.setVid(1001L);
        vo.setUid(7L);
        vo.setTitle("测试视频");
        vo.setCoverUrl("https://example.com/cover.webp");
        vo.setCoverThumbUrl("https://example.com/cover_thumb.webp");
        vo.setPlay(123L);
        vo.setCollect(9L);
        Page<VideoVo> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));
        when(videoService.pageItem(any(), any())).thenReturn(page);

        mockMvc.perform(get("/main/video/public/pageVideo")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("video-page.json"), false));
    }
}
