package com.video.communication.chat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.communication.chat.vo.ChatQueryVo;
import com.video.communication.chat.vo.ChatVo;
import com.video.model.entity.Chat;
import com.video.model.entity.ChatMessage;

/** 聊天室业务接口：聊天对象与消息的查询/落库/已读更新。 */
public interface ChatService {
    Chat getById(Long i);

    IPage<ChatVo> getChatVoPage(IPage<ChatVo> page, Long uid);

    void deleteChat(Long senderId, Long targetId);

    IPage<ChatMessage> getChatMessagePage(IPage<ChatMessage> page, Long senderId, Long targetId);

    ChatVo insertOneChat(Chat chat);

    void insertOneChatMessage(ChatMessage cm);

    ChatVo getChatVoByST(Long senderId, Long targetId);

    void update(ChatQueryVo q);

    void updateUnread(ChatQueryVo q);

    Integer getWhisperUnread(ChatQueryVo q);
}
