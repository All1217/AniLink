package com.video.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/** 管理端稿件列表/详情，附带作者昵称。 */
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date uploadDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deleteDate;
}
