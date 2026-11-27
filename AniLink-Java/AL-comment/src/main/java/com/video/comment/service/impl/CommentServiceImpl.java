package com.video.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.api.user.dto.UserBriefVo;
import com.video.comment.mapper.CommentMapper;
import com.video.comment.service.CommentService;
import com.video.comment.service.UserBriefService;
import com.video.comment.vo.CommentQueryVo;
import com.video.comment.vo.CommentVo;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.model.entity.Comment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 评论域业务实现：排序字段白名单、作者信息批量补全、只能删除本人评论。 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    /** sortField 会被拼进 order by，这里做白名单，避免 SQL 注入 */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("create_time", "love");

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserBriefService userBriefService;

    @Override
    public IPage<CommentVo> pageRootItem(IPage<CommentVo> page, CommentQueryVo queryVo) {
        // 只允许白名单内的列，非法值统一回退到按时间倒序
        if (queryVo.getSortField() != null && !ALLOWED_SORT_FIELDS.contains(queryVo.getSortField())) {
            log.warn("非法的评论排序字段：{}，已回退为 create_time", queryVo.getSortField());
            queryVo.setSortField("create_time");
        }
        IPage<CommentVo> result = commentMapper.pageRootItem(page, queryVo);
        enrichAuthors(result.getRecords());
        return result;
    }

    /** 评论作者昵称/头像改由 al-user 批量补全，避免跨域 JOIN user */
    private void enrichAuthors(List<CommentVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, UserBriefVo> users = userBriefService.byUids(
                records.stream().map(CommentVo::getUid).toList());
        records.forEach(comment -> {
            UserBriefVo user = users.get(comment.getUid());
            comment.setNickname(user == null ? null : user.getNickname());
            comment.setAvatar(user == null ? null : user.getAvatar());
        });
    }

    @Override
    public Long insertOne(Comment comment) {
        commentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    public IPage<CommentVo> pageReplyItem(IPage<CommentVo> page, CommentQueryVo queryVo) {
        IPage<CommentVo> result = commentMapper.pageReplyItem(page, queryVo);
        enrichAuthors(result.getRecords());
        return result;
    }

    @Override
    public int logicDelete(CommentQueryVo queryVo) {
        // 只允许删除自己的评论：条件里带上当前登录用户 uid，删不到就返回 0
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        Long uid = loginUser == null ? null : loginUser.getUserId();
        if (uid == null) {
            return 0;
        }
        Comment t = new Comment();
        t.setIsDeleted(1);
        UpdateWrapper<Comment> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", queryVo.getId());
        updateWrapper.eq("vid", queryVo.getVid());
        updateWrapper.eq("uid", uid);
        updateWrapper.eq("is_deleted", 0);
        return commentMapper.update(t, updateWrapper);
    }
}
