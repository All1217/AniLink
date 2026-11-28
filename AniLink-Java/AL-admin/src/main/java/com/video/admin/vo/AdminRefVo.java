package com.video.admin.vo;

import lombok.Data;

/** 存储对象被业务引用时的引用方。 */
@Data
public class AdminRefVo {
    private String type;
    private Long id;
    private String name;
    private String field;
}
