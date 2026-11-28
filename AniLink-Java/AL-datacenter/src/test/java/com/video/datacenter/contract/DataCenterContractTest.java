package com.video.datacenter.contract;

import com.video.datacenter.controller.DataCenterController;
import com.video.datacenter.service.DataCenterService;
import com.video.datacenter.vo.DataResVo;
import com.video.model.entity.HotSearch;
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
 * 数据中心接口契约快照：校验首页热搜列表与播放量趋势的对外 JSON 结构。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class DataCenterContractTest {

    @Mock
    private DataCenterService dataCenterService;

    @InjectMocks
    private DataCenterController dataCenterController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dataCenterController).build();
    }

    @Test
    void hotSearchKeepsListContract() throws Exception {
        // 首页热搜：算法引擎写入共享 Redis，BFF 原样返回 List<HotSearch>
        HotSearch hotSearch = new HotSearch();
        hotSearch.setKeyword("测试视频");
        hotSearch.setShowName("测试视频");
        hotSearch.setIcon("https://example.com/icon.png");
        hotSearch.setUri("/search?keyword=测试视频");
        hotSearch.setHeatScore(98765);
        when(dataCenterService.getHotSearch()).thenReturn(List.of(hotSearch));

        mockMvc.perform(get("/main/dataCenter/mainWindow/get/hotSearch"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("hot-search.json"), false));
    }

    @Test
    void totalPlayKeepsFourSeriesContract() throws Exception {
        // 播放量趋势：本期/上期的总量序列与粉丝量序列
        DataResVo vo = new DataResVo();
        vo.setMainRes(List.of(100L, 120L));
        vo.setSubRes(List.of(5L));
        vo.setLastMainRes(List.of(90L));
        vo.setLastSubRes(List.of(4L));
        when(dataCenterService.getTotalPlay(any(), any(), any())).thenReturn(vo);

        mockMvc.perform(get("/main/dataCenter/total/get/play")
                        .param("uid", "7")
                        .param("begin", "2026-01-01")
                        .param("end", "2026-01-07"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("total-play.json"), false));
    }
}
