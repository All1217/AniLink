package com.video.api.video.dto;

import lombok.Data;

import java.util.Date;

/** 跨服务视频摘要：收藏/历史/评论等域补全视频信息用。 */
@Data
public class VideoBriefVo {
    private Long vid;
    private Long uid;
    private String title;
    private Double duration;
    private String coverUrl;
    private String coverThumbUrl;
    private Long play;
    /** 上传时间：收藏/历史列表展示用，旧调用方不读该字段也兼容 */
    private Date uploadDate;
}

