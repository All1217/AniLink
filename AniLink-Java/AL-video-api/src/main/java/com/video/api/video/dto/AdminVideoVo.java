package com.video.api.video.dto;

import lombok.Data;

import java.util.Date;

/**
 * 管理端稿件视图（内部契约）：管理列表/详情字段，作者昵称由 al-video 通过用户域契约补全。
 */
@Data
public class AdminVideoVo {
    private Long vid;
    private Long uid;
    private String title;
    private Integer status;
    private Integer visible;
    private Integer transcodeStatus;
    private String coverUrl;
    private String videoUrl;
    private String hlsUrl;
    private String authorName;
    /** 上传/删除时间；跨服务按 Jackson 默认日期格式传输，对外输出格式由调用方 VO 决定 */
    private Date uploadDate;
    private Date deleteDate;
}
