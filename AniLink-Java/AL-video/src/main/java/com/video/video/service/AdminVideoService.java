package com.video.video.service;

import com.video.api.video.dto.AdminCoverThumbVo;
import com.video.api.video.dto.AdminVideoPageVo;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.api.video.dto.AdminVideoVo;

import java.util.List;

/**
 * 视频域管理契约服务：AL-admin 的稿件审核/可见性/存储引用/封面缩略图回填都走这里。
 * 管理端的状态流转规则属于 admin 业务规则，不在本服务内实现。
 */
public interface AdminVideoService {

    /** 稿件分页：不传 status 时默认排除已删除（status=3） */
    AdminVideoPageVo page(long current, long size, Integer status, Integer visible, Long uid, String keyword);

    /** 稿件详情（含作者昵称）；不存在返回 null */
    AdminVideoVo detail(Long vid);

    /** 更新审核/删除状态；目标为 3 时写 delete_date，其余清空 */
    void updateStatus(Long vid, Integer status);

    /** 更新公开/仅自己可见 */
    void updateVisible(Long vid, Integer visible);

    /** 按完整 URL 查稿件字段引用 */
    List<AdminVideoRefVo> refsByUrl(String url);

    /** HLS 目录引用 */
    AdminVideoRefVo hlsRef(Long vid);

    /** 按 vid 游标取缺少封面缩略图的稿件 */
    List<AdminCoverThumbVo> missingThumbs(long afterVid, int limit);

    /** 写回封面缩略图，仅当前值为空时生效；返回更新行数 */
    int updateThumb(Long vid, String thumbUrl);
}
