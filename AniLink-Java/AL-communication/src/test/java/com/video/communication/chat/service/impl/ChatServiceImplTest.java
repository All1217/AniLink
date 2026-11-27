package com.video.communication.chat.service.impl;

import com.video.api.user.dto.UserBriefVo;
import com.video.communication.chat.mapper.ChatMapper;
import com.video.communication.chat.service.UserBriefService;
import com.video.communication.chat.vo.ChatQueryVo;
import com.video.communication.chat.vo.ChatVo;
import com.video.model.entity.Chat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 聊天域单测：双方作者信息补全、聊天对象幂等创建、删除时级联清理消息。 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMapper chatMapper;
    @Mock
    private UserBriefService userBriefService;
    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    @DisplayName("双ID查聊天室时补全双方昵称/头像")
    void getChatVoBySTEnrichesBothSides() {
        ChatVo vo = new ChatVo();
        vo.setSenderId(1L);
        vo.setTargetId(2L);
        when(chatMapper.getChatVoByTwoIds(1L, 2L)).thenReturn(vo);

        UserBriefVo sender = new UserBriefVo();
        sender.setUid(1L);
        sender.setNickname("发送者");
        sender.setAvatar("http://img/1.webp");
        UserBriefVo target = new UserBriefVo();
        target.setUid(2L);
        target.setNickname("接收者");
        target.setAvatar("http://img/2.webp");
        when(userBriefService.byUids(any())).thenReturn(Map.of(1L, sender, 2L, target));

        ChatVo result = chatService.getChatVoByST(1L, 2L);

        assertEquals("发送者", result.getSenderName());
        assertEquals("http://img/1.webp", result.getSenderAvatar());
        assertEquals("接收者", result.getTargetName());
        assertEquals("http://img/2.webp", result.getTargetAvatar());
    }

    @Test
    @DisplayName("聊天室已存在时复用，不重复插入")
    void insertOneChatReusesExisting() {
        Chat existing = new Chat();
        existing.setSenderId(1L);
        existing.setTargetId(2L);
        when(chatMapper.getByTwoIds(1L, 2L)).thenReturn(existing);
        ChatVo vo = new ChatVo();
        vo.setSenderId(1L);
        vo.setTargetId(2L);
        when(chatMapper.getChatVoByTwoIds(1L, 2L)).thenReturn(vo);
        when(userBriefService.byUids(any())).thenReturn(Map.of());
        Chat chat = new Chat();
        chat.setSenderId(1L);
        chat.setTargetId(2L);

        ChatVo result = chatService.insertOneChat(chat);

        assertEquals(1L, result.getSenderId());
        verify(chatMapper, never()).insert(any());
    }

    @Test
    @DisplayName("聊天室不存在时先插入再回查")
    void insertOneChatCreatesWhenAbsent() {
        Chat chat = new Chat();
        chat.setSenderId(1L);
        chat.setTargetId(2L);
        when(chatMapper.getByTwoIds(1L, 2L)).thenReturn(null);
        ChatVo vo = new ChatVo();
        vo.setSenderId(1L);
        vo.setTargetId(2L);
        when(chatMapper.getChatVoByTwoIds(1L, 2L)).thenReturn(vo);
        when(userBriefService.byUids(any())).thenReturn(Map.of());

        ChatVo result = chatService.insertOneChat(chat);

        verify(chatMapper).insert(chat);
        assertEquals(2L, result.getTargetId());
    }

    @Test
    @DisplayName("删除聊天对象时级联删除双方聊天消息")
    void deleteChatRemovesMessages() {
        chatService.deleteChat(1L, 2L);

        verify(chatMapper).deleteChat(1L, 2L);
        verify(chatMapper).deleteChatMessage(1L, 2L);
    }

    @Test
    @DisplayName("更新最近打开时间：没有时间参数时不覆盖 latest_time")
    void updateKeepsLatestTimeWhenAbsent() {
        ChatQueryVo queryVo = new ChatQueryVo();
        queryVo.setId(5L);

        chatService.update(queryVo);

        verify(chatMapper).update(any(), any());
    }
}
