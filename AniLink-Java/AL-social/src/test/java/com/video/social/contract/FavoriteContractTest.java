package com.video.social.contract;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.model.entity.Favorite;
import com.video.social.controller.FavoriteController;
import com.video.social.service.FavoriteService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 收藏夹列表接口契约快照：只校验网关对外 JSON 结构（code/message/data.records）。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class FavoriteContractTest {

    @Mock
    private FavoriteService favoriteService;

    @InjectMocks
    private FavoriteController favoriteController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(favoriteController).build();
    }

    @Test
    void favoriteListKeepsPagedRecordsContract() throws Exception {
        // 构造一个收藏夹：收藏夹列表由本域直接返回 Favorite 实体
        Favorite favorite = new Favorite();
        favorite.setFid(301L);
        favorite.setUid(7L);
        favorite.setType(0);
        favorite.setVisible(1);
        favorite.setTitle("默认收藏夹");
        favorite.setCount(12);
        Page<Favorite> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(favorite));
        when(favoriteService.getFavoriteListPage(any(), anyLong())).thenReturn(page);

        mockMvc.perform(get("/main/favorite/getFavoriteListPage")
                        .param("current", "1")
                        .param("size", "10")
                        .param("uid", "7"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("favorite-list.json"), false));
    }
}
