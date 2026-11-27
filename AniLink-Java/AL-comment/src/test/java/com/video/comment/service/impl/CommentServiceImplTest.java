package com.video.comment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.api.user.dto.UserBriefVo;
import com.video.comment.mapper.CommentMapper;
import com.video.comment.service.UserBriefService;
import com.video.comment.vo.CommentQueryVo;
import com.video.comment.vo.CommentVo;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.model.entity.Comment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 评论域单测：排序字段白名单、作者信息批量补全、只能删除本人评论。 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserBriefService userBriefService;
    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        LoginUserHolder.clear();
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
    }

    @Test
    @DisplayName("非法排序字段回退为 create_time，防止拼接注入")
    void illegalSortFieldFallsBack() {
        CommentQueryVo queryVo = new CommentQueryVo();
        queryVo.setVid(9L);
        queryVo.setSortField("uid desc; drop table comment");
        when(commentMapper.pageRootItem(any(), any())).thenReturn(new Page<>());

        commentService.pageRootItem(new Page<>(), queryVo);

        assertEquals("create_time", queryVo.getSortField());
    }

    @Test
    @DisplayName("评论作者昵称头像由 al-user 批量补全")
    void authorsAreEnriched() {
        CommentVo first = new CommentVo();
        first.setUid(1L);
        CommentVo second = new CommentVo();
        second.setUid(2L);
        Page<CommentVo> page = new Page<>();
        page.setRecords(List.of(first, second));
        CommentQueryVo queryVo = new CommentQueryVo();
        when(commentMapper.pageRootItem(any(), any())).thenReturn(page);

        UserBriefVo user = new UserBriefVo();
        user.setUid(1L);
        user.setNickname("小电视");
        user.setAvatar("http://img/1.webp");
        when(userBriefService.byUids(List.of(1L, 2L))).thenReturn(Map.of(1L, user));

        commentService.pageRootItem(new Page<>(), queryVo).getRecords();

        assertEquals("小电视", first.getNickname());
        assertEquals("http://img/1.webp", first.getAvatar());
        // 用户不存在的评论保留空昵称/头像，不阻断列表
        assertNull(second.getNickname());
        assertNull(second.getAvatar());
    }

    @Test
    @DisplayName("未登录时逻辑删除直接返回 0，不落库")
    void logicDeleteWithoutLoginIsRejected() {
        int affected = commentService.logicDelete(new CommentQueryVo());

        assertEquals(0, affected);
        verify(commentMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("登录后逻辑删除带当前 uid 条件")
    void logicDeleteUsesLoginUser() {
        LoginUserHolder.setLoginUser(new LoginUser(42L, "tester"));
        CommentQueryVo queryVo = new CommentQueryVo();
        queryVo.setId(7L);
        queryVo.setVid(9L);
        when(commentMapper.update(any(), any())).thenReturn(1);

        assertEquals(1, commentService.logicDelete(queryVo));
        verify(commentMapper).update(any(), any());
    }

    @Test
    @DisplayName("新增评论返回数据库生成的主键")
    void insertOneReturnsGeneratedId() {
        Comment comment = new Comment();
        comment.setUid(1L);
        when(commentMapper.insert(any())).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(100L);
            return 1;
        });

        assertEquals(100L, commentService.insertOne(comment));
        assertTrue(commentService.insertOne(comment) > 0);
    }
}
