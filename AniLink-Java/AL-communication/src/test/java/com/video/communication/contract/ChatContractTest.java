package com.video.communication.contract;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.communication.chat.controller.ChatController;
import com.video.communication.chat.service.ChatService;
import com.video.communication.chat.vo.ChatVo;
import com.video.model.entity.Chat;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 契约补测：私信（聊天）分页与创建聊天对象接口的对外 JSON 契约。
 * MockMvc standalone，不依赖 Nacos/MySQL/Netty，避免运行态环境干扰。
 */
@ExtendWith(MockitoExtension.class)
class ChatContractTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Test
    @DisplayName("聊天对象分页返回 code=200 + records/total 结构")
    void chatVoPageKeepsPagedContract() throws Exception {
        ChatVo vo = new ChatVo();
        vo.setId(1L);
        vo.setSenderId(7L);
        vo.setTargetId(8L);
        vo.setSenderName("发送者");
        vo.setTargetName("接收者");
        vo.setUnreadCount(2);
        Page<ChatVo> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(vo));
        when(chatService.getChatVoPage(any(), any())).thenReturn(page);

        mockMvc.perform(get("/main/chat/getChatVoPage")
                        .param("current", "1").param("size", "10").param("uid", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].senderId").value(7))
                .andExpect(jsonPath("$.data.records[0].targetName").value("接收者"));
    }

    @Test
    @DisplayName("创建聊天对象返回 code=200 + ChatVo")
    void insertOneChatKeepsVoContract() throws Exception {
        ChatVo vo = new ChatVo();
        vo.setId(2L);
        vo.setSenderId(7L);
        vo.setTargetId(8L);
        when(chatService.insertOneChat(any(Chat.class))).thenReturn(vo);

        mockMvc.perform(post("/main/chat/insertOneChat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderId\":7,\"targetId\":8,\"latestTime\":\"2026-11-28 22:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.senderId").value(7))
                .andExpect(jsonPath("$.data.targetId").value(8));
    }
}
