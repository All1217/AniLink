package com.video.api.user.dto;

import lombok.Data;

/** 按标签名取用户 id 的查询：只取权重 &gt; 0 的标签（与旧弹幕标签过滤 SQL 一致）。 */
@Data
public class TagUsersQueryVo {
    /** 标签名 */
    private String tagName;
}
