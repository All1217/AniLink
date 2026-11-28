package com.video.api.video.dto;

import lombok.Data;

/**
 * 视频侧存储对象引用（内部契约）：对象存储删除前查该 URL 是否仍被稿件字段引用。
 */
@Data
public class AdminVideoRefVo {
    /** 引用方类型，固定 video */
    private String type;
    /** 稿件 vid */
    private Long id;
    /** 稿件标题 */
    private String name;
    /** 引用字段：video_url / cover_url / cover_thumb_url / hls_url */
    private String field;
}
