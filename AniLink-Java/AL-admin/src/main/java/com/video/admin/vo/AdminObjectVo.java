package com.video.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/** 存储对象列表项。 */
@Data
public class AdminObjectVo {
    private String bucket;
    private String key;
    private long size;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastModified;
}
