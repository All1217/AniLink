package com.video.web.main.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.Comment;
import com.video.web.main.vo.CommentQueryVo;
import com.video.web.main.vo.CommentVo;

public interface CommentService {
    IPage<CommentVo> pageRootItem(IPage<CommentVo> page, CommentQueryVo queryVo);

    Long insertOne(Comment comment);

    IPage<CommentVo> pageReplyItem(IPage<CommentVo> page, CommentQueryVo queryVo);

    /** 逻辑删除；返回受影响行数（0 表示不存在或不是本人评论） */
    int logicDelete(CommentQueryVo queryVo);
}
