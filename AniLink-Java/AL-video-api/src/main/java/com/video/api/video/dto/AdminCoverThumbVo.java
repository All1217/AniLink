package com.video.api.video.dto;

import lombok.Data;

/**
 * 缺封面缩略图的稿件（内部契约）：管理端回填时按 vid 游标分批消费。
 */
@Data
public class AdminCoverThumbVo {
    private Long vid;
    /** 960w 封面主图地址，回填时据此派生 480w 缩略图 */
    private String coverUrl;
}
