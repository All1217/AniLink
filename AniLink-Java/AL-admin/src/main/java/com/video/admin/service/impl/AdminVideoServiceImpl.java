package com.video.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.admin.service.AdminVideoService;
import com.video.admin.vo.AdminVideoQueryVo;
import com.video.admin.vo.AdminVideoVo;
import com.video.api.video.AdminVideoInternalClient;
import com.video.api.video.dto.AdminVideoPageVo;
import com.video.common.exception.VideoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 稿件管理：只改逻辑状态，不触碰 MinIO/转码。
 * Phase 5 起 video 表归 al-video，查询与状态写入经 AdminVideoInternalClient，管理端不再直连。
 */
@Slf4j
@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    private final AdminVideoInternalClient adminVideoClient;

    public AdminVideoServiceImpl(AdminVideoInternalClient adminVideoClient) {
        this.adminVideoClient = adminVideoClient;
    }

    @Override
    public IPage<AdminVideoVo> page(AdminVideoQueryVo query) {
        long current = Math.max(1, query.getCurrent());
        long size = Math.min(200, Math.max(1, query.getSize()));
        AdminVideoPageVo remote = adminVideoClient.page(current, size,
                query.getStatus(), null, query.getUid(), query.getKeyword());
        Page<AdminVideoVo> page = new Page<>(remote.getCurrent(), remote.getSize(), remote.getTotal());
        List<AdminVideoVo> records = remote.getRecords() == null ? List.of()
                : remote.getRecords().stream().map(this::toVo).toList();
        page.setRecords(records);
        return page;
    }

    @Override
    public AdminVideoVo detail(Long vid) {
        AdminVideoVo vo = toVo(adminVideoClient.detail(vid));
        if (vo == null) {
            throw new VideoException(404, "稿件不存在");
        }
        return vo;
    }

    @Override
    public void updateStatus(Long vid, Integer status, Long operatorUid) {
        if (status == null) {
            throw new VideoException(202, "缺少目标状态");
        }
        // 状态流转校验属于管理端规则，先取视频域详情再判断，写回交给视频域契约
        com.video.api.video.dto.AdminVideoVo video = adminVideoClient.detail(vid);
        if (video == null) {
            throw new VideoException(404, "稿件不存在");
        }
        int from = video.getStatus() == null ? -1 : video.getStatus();
        boolean allowed = (from == 0 && (status == 1 || status == 2))
                || ((from == 1 || from == 2) && status == 3);
        if (!allowed) {
            throw new VideoException(409, "稿件状态不允许该操作");
        }
        adminVideoClient.updateStatus(vid, status);
        log.info("admin action operator={} action=video.status vid={} from={} to={}",
                operatorUid, vid, from, status);
    }

    @Override
    public void updateVisible(Long vid, Integer visible, Long operatorUid) {
        if (visible == null || (visible != 0 && visible != 1)) {
            throw new VideoException(202, "visible 只能是 0 或 1");
        }
        if (adminVideoClient.detail(vid) == null) {
            throw new VideoException(404, "稿件不存在");
        }
        adminVideoClient.updateVisible(vid, visible);
        log.info("admin action operator={} action=video.visible vid={} to={}",
                operatorUid, vid, visible);
    }

    /** 视频域契约 DTO → 管理端对外 VO，字段保持一致 */
    private AdminVideoVo toVo(com.video.api.video.dto.AdminVideoVo source) {
        if (source == null) {
            return null;
        }
        AdminVideoVo vo = new AdminVideoVo();
        vo.setVid(source.getVid());
        vo.setUid(source.getUid());
        vo.setTitle(source.getTitle());
        vo.setStatus(source.getStatus());
        vo.setVisible(source.getVisible());
        vo.setTranscodeStatus(source.getTranscodeStatus());
        vo.setCoverUrl(source.getCoverUrl());
        vo.setVideoUrl(source.getVideoUrl());
        vo.setHlsUrl(source.getHlsUrl());
        vo.setAuthorName(source.getAuthorName());
        vo.setUploadDate(source.getUploadDate());
        vo.setDeleteDate(source.getDeleteDate());
        return vo;
    }
}
