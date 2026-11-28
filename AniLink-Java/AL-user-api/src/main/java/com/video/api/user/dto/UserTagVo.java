package com.video.api.user.dto;

import lombok.Data;

/** 跨服务用户标签行：数据中心在内存里做标签统计，不需要暴露 user_tag 主键/时间等字段。 */
@Data
public class UserTagVo {
    /** 用户 id */
    private Long uid;
    /** 标签名 */
    private String tagName;
    /** 标签权重 */
    private Double weight;
}
