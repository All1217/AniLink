package com.video.comment.vo;

import com.video.model.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 评论视图：在实体基础上补充展示用的作者昵称/头像与回复数。 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "评论(Vo)")
public class CommentVo extends Comment {
    @Schema(description="用户昵称")
    private String nickname;

    @Schema(description="头像Url")
    private String avatar;

    @Schema(description="回复数量")
    private Integer replyCount;
}
