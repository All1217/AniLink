package com.video.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.comment.vo.CommentQueryVo;
import com.video.comment.vo.CommentVo;
import com.video.model.entity.Comment;

/** 评论 Mapper：只访问 comment 表，不 JOIN 其他域的表。 */
public interface CommentMapper extends BaseMapper<Comment> {
    IPage<CommentVo> pageRootItem(IPage<CommentVo> page, CommentQueryVo queryVo);

    IPage<CommentVo> pageReplyItem(IPage<CommentVo> page, CommentQueryVo queryVo);
}
