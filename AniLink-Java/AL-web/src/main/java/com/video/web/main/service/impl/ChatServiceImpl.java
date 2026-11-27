package com.video.web.main.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.Chat;
import com.video.model.entity.ChatMessage;
import com.video.web.main.mapper.ChatMapper;
import com.video.web.main.service.ChatService;
import com.video.web.main.service.UserBriefService;
import com.video.web.main.vo.ChatQueryVo;
import com.video.web.main.vo.ChatVo;
import com.video.api.user.dto.UserBriefVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private UserBriefService userBriefService;

    @Override
    public Chat getById(Long i) {
        return chatMapper.selectById(i);
    }

    @Override
    @Transactional
    public IPage<ChatVo> getChatVoPage(IPage<ChatVo> page, Long uid) {
        IPage<ChatVo> result = chatMapper.getChatVoPage(page, uid);
        enrich(result.getRecords());
        return result;
    }

    /** 双方昵称/头像改由 al-user 批量补全，避免跨域 JOIN user */
    private void enrich(List<ChatVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> uids = new ArrayList<>();
        records.forEach(chat -> {
            uids.add(chat.getSenderId());
            uids.add(chat.getTargetId());
        });
        Map<Long, UserBriefVo> users = userBriefService.byUids(uids);
        records.forEach(chat -> {
            UserBriefVo sender = users.get(chat.getSenderId());
            UserBriefVo target = users.get(chat.getTargetId());
            chat.setSenderName(sender == null ? null : sender.getNickname());
            chat.setSenderAvatar(sender == null ? null : sender.getAvatar());
            chat.setTargetName(target == null ? null : target.getNickname());
            chat.setTargetAvatar(target == null ? null : target.getAvatar());
        });
    }

    @Override
    @Transactional
    public void deleteChat(Long senderId, Long targetId) {
        chatMapper.deleteChat(senderId, targetId);
        //删除相关聊天信息
        chatMapper.deleteChatMessage(senderId, targetId);
    }

    @Override
    public IPage<ChatMessage> getChatMessagePage(IPage<ChatMessage> page, Long senderId, Long targetId) {
        return chatMapper.getChatMessagePage(page, senderId, targetId);
    }

    @Override
    @Transactional
    public ChatVo insertOneChat(Chat chat) {
        Chat t = chatMapper.getByTwoIds(chat.getSenderId(), chat.getTargetId());
        ChatVo res;
        if (t == null) {
            chatMapper.insert(chat);
            res = chatMapper.getChatVoByTwoIds(chat.getSenderId(), chat.getTargetId());
        } else {
            res = chatMapper.getChatVoByTwoIds(t.getSenderId(), t.getTargetId());
        }
        if (res != null) {
            enrich(java.util.Collections.singletonList(res));
        }
        return res;
    }

    @Override
    public void insertOneChatMessage(ChatMessage cm) {
        chatMapper.insertOneChatMessage(cm);
    }

    @Override
    public ChatVo getChatVoByST(Long senderId, Long targetId) {
        ChatVo res = chatMapper.getChatVoByTwoIds(senderId, targetId);
        if (res != null) {
            enrich(java.util.Collections.singletonList(res));
        }
        return res;
    }

    @Override
    public void update(ChatQueryVo q) {
        UpdateWrapper<Chat> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", q.getId());
        Chat newChat = new Chat();
        if (q.getTime() != null) newChat.setLatestTime(q.getTime());
        chatMapper.update(newChat, updateWrapper);
    }

    @Override
    public void updateUnread(ChatQueryVo q) {
        chatMapper.updateUnread(q);
    }

    @Override
    public Integer getWhisperUnread(ChatQueryVo q) {
        return chatMapper.getWhisperUnread(q);
    }
}
