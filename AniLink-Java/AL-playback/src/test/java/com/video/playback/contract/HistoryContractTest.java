package com.video.playback.contract;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.playback.controller.HistoryController;
import com.video.playback.service.HistoryService;
import com.video.playback.vo.HistoryVo;
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
 * 历史分页接口契约快照：只校验网关对外 JSON 结构（code/message/data.records）。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class HistoryContractTest {

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private HistoryController historyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(historyController).build();
    }

    @Test
    void historyPageKeepsPagedRecordsContract() throws Exception {
        // 构造一条历史记录：实体字段 + al-video/al-user 补全的展示字段
        HistoryVo vo = new HistoryVo();
        vo.setId(501L);
        vo.setUid(7L);
        vo.setVid(1001L);
        vo.setNickname("alice");
        vo.setTitle("测试视频");
        vo.setCoverUrl("https://example.com/cover.webp");
        vo.setCoverThumbUrl("https://example.com/cover_thumb.webp");
        Page<HistoryVo> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));
        when(historyService.getHistoryPage(any(), any())).thenReturn(page);

        mockMvc.perform(get("/main/history/getHistory/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("history-page.json"), false));
    }
}
