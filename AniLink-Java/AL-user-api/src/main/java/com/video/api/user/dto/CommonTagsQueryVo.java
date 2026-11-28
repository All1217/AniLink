package com.video.api.user.dto;

import lombok.Data;

/** 共同标签查询：uid 与 otherUid 都是用户 id（旧接口把 vid 当另一个用户 id 用）。 */
@Data
public class CommonTagsQueryVo {
    /** 用户 id */
    private Long uid;
    /** 另一个用户 id（旧字段名沿用 vid） */
    private Long vid;
}
