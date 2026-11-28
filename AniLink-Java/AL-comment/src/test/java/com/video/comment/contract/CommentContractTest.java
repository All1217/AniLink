package com.video.comment.contract;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.comment.controller.CommentController;
import com.video.comment.service.CommentService;
import com.video.comment.vo.CommentVo;
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
 * 评论分页接口契约快照：只校验网关对外 JSON 结构（code/message/data.records）。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class CommentContractTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
    }

    @Test
    void rootCommentPageKeepsPagedRecordsContract() throws Exception {
        // 构造一条根评论：实体字段 + al-user 补全的昵称/头像 + 回复数量
        CommentVo vo = new CommentVo();
        vo.setId(9001L);
        vo.setVid(1001L);
        vo.setUid(7L);
        vo.setContent("测试评论");
        vo.setLove(3);
        vo.setNickname("alice");
        vo.setAvatar("https://example.com/avatar.webp");
        vo.setReplyCount(2);
        Page<CommentVo> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));
        when(commentService.pageRootItem(any(), any())).thenReturn(page);

        mockMvc.perform(get("/main/comment/pageCommentVo")
                        .param("current", "1")
                        .param("size", "10")
                        .param("vid", "1001"))
                .andExpect(status().isOk())
                .andExpect(content().json(ContractSnapshot.read("comment-page.json"), false));
    }
}
