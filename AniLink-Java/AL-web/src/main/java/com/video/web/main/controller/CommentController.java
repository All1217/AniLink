package com.video.web.main.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.result.Result;
import com.video.model.entity.Comment;
import com.video.web.main.service.CommentService;
import com.video.web.main.vo.CommentQueryVo;
import com.video.web.main.vo.CommentVo;
import com.video.web.main.vo.RegisterVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import static com.video.common.result.ResultCodeEnum.PARAM_ERROR;

@Tag(name = "评论相关接口")
@RestController
@RequestMapping("/main/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Operation(summary = "查询根评论")
    @GetMapping("/pageCommentVo")
    public Result<IPage<CommentVo>> getPageRootComment(@RequestParam long current, @RequestParam long size, CommentQueryVo queryVo) {
        IPage<CommentVo> page = new Page<>(current, size);
        IPage<CommentVo> result = commentService.pageRootItem(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "查询回复")
    @GetMapping("/pageReply")
    public Result<IPage<CommentVo>> getPageReplyComment(@RequestParam long current, @RequestParam long size, CommentQueryVo queryVo) {
        IPage<CommentVo> page = new Page<>(current, size);
        IPage<CommentVo> result = commentService.pageReplyItem(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "新增评论")
    @PostMapping("/insert")
    public Result<Long> saveOrUpdate(@RequestBody Comment comment) {
        Long id = commentService.insertOne(comment);
        return Result.ok(id);
    }

    @Operation(summary = "逻辑删除评论")
    @PutMapping("/logicDelete")
    public Result logicDelete(@RequestBody CommentQueryVo queryVo) {
        // 只能删除自己的评论：service 里已带 uid 条件，删不到即视为无权限
        if (commentService.logicDelete(queryVo) == 0) {
            return Result.fail(PARAM_ERROR.getCode(), "只能删除自己的评论");
        }
        return Result.ok();
    }
}
