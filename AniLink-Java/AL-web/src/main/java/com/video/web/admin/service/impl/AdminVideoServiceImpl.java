package com.video.web.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.exception.VideoException;
import com.video.model.entity.Video;
import com.video.web.admin.mapper.AdminVideoMapper;
import com.video.web.admin.service.AdminVideoService;
import com.video.web.admin.vo.AdminVideoQueryVo;
import com.video.web.admin.vo.AdminVideoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/** 稿件管理：只改逻辑状态，不触碰 MinIO/转码。 */
@Slf4j
@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    private final AdminVideoMapper adminVideoMapper;

    public AdminVideoServiceImpl(AdminVideoMapper adminVideoMapper) {
        this.adminVideoMapper = adminVideoMapper;
    }

    @Override
    public IPage<AdminVideoVo> page(AdminVideoQueryVo query) {
        long current = Math.max(1, query.getCurrent());
        long size = Math.min(200, Math.max(1, query.getSize()));
        return adminVideoMapper.pageAdmin(new Page<>(current, size), query);
    }

    @Override
    public AdminVideoVo detail(Long vid) {
        AdminVideoVo vo = adminVideoMapper.getAdminDetail(vid);
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
        // 管理端是 video 表的过渡读方：状态校验走 AdminVideoMapper，不依赖视频域 Mapper
        Video video = adminVideoMapper.selectVideoStatus(vid);
        if (video == null) {
            throw new VideoException(404, "稿件不存在");
        }
        int from = video.getStatus() == null ? -1 : video.getStatus();
        boolean allowed = (from == 0 && (status == 1 || status == 2))
                || ((from == 1 || from == 2) && status == 3);
        if (!allowed) {
            throw new VideoException(409, "稿件状态不允许该操作");
        }
        Date deleteDate = status == 3 ? new Date() : null;
        adminVideoMapper.updateStatus(vid, status, deleteDate);
        log.info("admin action operator={} action=video.status vid={} from={} to={}",
                operatorUid, vid, from, status);
    }

    @Override
    public void updateVisible(Long vid, Integer visible, Long operatorUid) {
        if (visible == null || (visible != 0 && visible != 1)) {
            throw new VideoException(202, "visible 只能是 0 或 1");
        }
        Video video = adminVideoMapper.selectVideoStatus(vid);
        if (video == null) {
            throw new VideoException(404, "稿件不存在");
        }
        adminVideoMapper.updateVisible(vid, visible);
        log.info("admin action operator={} action=video.visible vid={} to={}",
                operatorUid, vid, visible);
    }
}
