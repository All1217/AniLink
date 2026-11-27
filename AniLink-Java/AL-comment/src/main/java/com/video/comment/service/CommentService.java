package com.video.comment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.comment.vo.CommentQueryVo;
import com.video.comment.vo.CommentVo;
import com.video.model.entity.Comment;

/** 评论域业务接口：分页查询、新增与逻辑删除。 */
public interface CommentService {
    IPage<CommentVo> pageRootItem(IPage<CommentVo> page, CommentQueryVo queryVo);

    Long insertOne(Comment comment);

    IPage<CommentVo> pageReplyItem(IPage<CommentVo> page, CommentQueryVo queryVo);

    /** 逻辑删除；返回受影响行数（0 表示不存在或不是本人评论） */
    int logicDelete(CommentQueryVo queryVo);
}
