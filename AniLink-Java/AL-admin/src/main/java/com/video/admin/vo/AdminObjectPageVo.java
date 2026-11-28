package com.video.admin.vo;

import lombok.Data;

import java.util.List;

/** 存储对象分页结果。 */
@Data
public class AdminObjectPageVo {
    private List<AdminObjectVo> items;
    private String nextCursor;
    private boolean hasMore;
}
