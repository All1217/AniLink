package com.video.communication.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.result.Result;
import com.video.communication.chat.service.ChatService;
import com.video.communication.chat.vo.ChatQueryVo;
import com.video.communication.chat.vo.ChatVo;
import com.video.model.entity.Chat;
import com.video.model.entity.ChatMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 聊天室接口：路径保持 /main/chat/** 不变，HTTP 落库与 /chat WS 的先后时序不变。 */
@Tag(name = "聊天室接口")
@RestController
@RequestMapping("/main/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @Operation(summary = "按ID获取聊天室信息")
    @GetMapping("/info")
    public Result<Chat> info() {
        Chat chat = chatService.getById(1L);
        return Result.ok(chat);
    }

    @Operation(summary = "双ID获取聊天室（vo）")
    @GetMapping("/get/chat/st")
    public Result<ChatVo> getChatVoByST(@RequestParam Long senderId, @RequestParam Long targetId) {
        ChatVo res = chatService.getChatVoByST(senderId, targetId);
        return Result.ok(res);
    }

    @Operation(summary = "分页获取聊天对象")
    @GetMapping("/getChatVoPage")
    public Result<IPage<ChatVo>> getChatVoPage(@RequestParam Long current,
                                               @RequestParam Long size,
                                               @RequestParam Long uid) {
        IPage<ChatVo> page = new Page<>(current, size);
        IPage<ChatVo> res = chatService.getChatVoPage(page, uid);
        return Result.ok(res);
    }

    @Operation(summary = "删除聊天对象")
    @DeleteMapping("/deleteChat")
    public Result deleteChat(@RequestBody ChatQueryVo query) {
        chatService.deleteChat(query.getSenderId(), query.getTargetId());
        return Result.ok();
    }

    @Operation(summary = "分页获取聊天信息")
    @GetMapping("/getChatMessagePage")
    public Result<IPage<ChatMessage>> getChatMessagePage(@RequestParam Long current,
                                                         @RequestParam Long size,
                                                         @RequestParam Long senderId,
                                                         @RequestParam Long targetId) {
        IPage<ChatMessage> page = new Page<>(current, size);
        IPage<ChatMessage> res = chatService.getChatMessagePage(page, senderId, targetId);
        return Result.ok(res);
    }

    @Operation(summary = "创建聊天对象")
    @PostMapping("/insertOneChat")
    public Result<ChatVo> insertOneChat(@RequestBody Chat chat) {
        ChatVo res = chatService.insertOneChat(chat);
        return Result.ok(res);
    }

    @Operation(summary = "插入新消息")
    @PostMapping("/insertOneChatMessage")
    public Result insertOneChatMessage(@RequestBody ChatMessage cm) {
        chatService.insertOneChatMessage(cm);
        return Result.ok();
    }

    @Operation(summary = "更新最近打开时间")
    @PutMapping("/update")
    public Result update(@RequestBody ChatQueryVo q) {
        chatService.update(q);
        return Result.ok();
    }

    @Operation(summary = "批量更新未读消息")
    @PutMapping("/update/unread")
    public Result updateUnread(@RequestBody ChatQueryVo q) {
        chatService.updateUnread(q);
        return Result.ok();
    }

    @Operation(summary = "获取全局未读消息数量")
    @GetMapping("/getWhisperUnread")
    public Result<Integer> getWhisperUnread(ChatQueryVo q) {
        Integer res = chatService.getWhisperUnread(q);
        return Result.ok(res);
    }
}
