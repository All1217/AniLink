package com.video.comment.contract;

import com.video.comment.controller.CommentController;
import com.video.comment.service.CommentService;
import com.video.model.entity.Comment;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 契约补测：评论新增接口的对外 JSON 契约（code/message/data=新评论 id）。
 * MockMvc standalone，不依赖 Nacos/MySQL/Redis，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class CommentInsertContractTest {

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
    @DisplayName("新增评论返回 code=200 + data=评论 id")
    void insertCommentKeepsIdContract() throws Exception {
        when(commentService.insertOne(any(Comment.class))).thenReturn(9101L);

        mockMvc.perform(post("/main/comment/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vid\":1001,\"uid\":7,\"rootId\":0,\"parentId\":0,\"toUserId\":0,"
                                + "\"content\":\"契约测试评论\",\"createTime\":\"2026-11-28 22:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data").value(9101));
    }
}
