package com.video.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.api.user.dto.UserBriefVo;
import com.video.api.video.dto.AdminCoverThumbVo;
import com.video.api.video.dto.AdminVideoPageVo;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.api.video.dto.AdminVideoVo;
import com.video.common.exception.VideoException;
import com.video.model.entity.Video;
import com.video.video.mapper.VideoMapper;
import com.video.video.service.AdminVideoService;
import com.video.video.service.UserBriefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 视频域管理契约实现：管理端不再 JOIN user / 直连 video 表，
 * 作者昵称通过用户域内部契约补全，SQL 全部收敛在视频域自己的 Mapper。
 */
@Slf4j
@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    /** 分页大小上限，避免内部调用方一次拉全表 */
    private static final long MAX_PAGE_SIZE = 200L;
    /** 管理端「已删除」状态，默认列表要排除 */
    private static final int STATUS_DELETED = 3;

    private final VideoMapper videoMapper;
    private final UserBriefService userBriefService;

    public AdminVideoServiceImpl(VideoMapper videoMapper, UserBriefService userBriefService) {
        this.videoMapper = videoMapper;
        this.userBriefService = userBriefService;
    }

    @Override
    public AdminVideoPageVo page(long current, long size, Integer status, Integer visible,
                                 Long uid, String keyword) {
        long safeCurrent = Math.max(1, current);
        long safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        LambdaQueryWrapper<Video> wrapper = Wrappers.lambdaQuery();
        // 审核状态：传了就精确过滤，没传默认排除已删除，与原管理端语义一致
        if (status != null) {
            wrapper.eq(Video::getStatus, status);
        } else {
            wrapper.ne(Video::getStatus, STATUS_DELETED);
        }
        wrapper.eq(visible != null, Video::getVisible, visible)
                .eq(uid != null, Video::getUid, uid)
                .like(keyword != null && !keyword.isBlank(), Video::getTitle, keyword)
                .orderByDesc(Video::getUploadDate)
                .orderByDesc(Video::getVid);
        IPage<Video> result = videoMapper.selectPage(new Page<>(safeCurrent, safeSize), wrapper);
        Map<Long, UserBriefVo> authors = userBriefService.byUids(
                result.getRecords().stream().map(Video::getUid).toList());
        AdminVideoPageVo vo = new AdminVideoPageVo();
        vo.setRecords(result.getRecords().stream()
                .map(video -> toVo(video, nicknameOf(authors, video.getUid())))
                .toList());
        vo.setTotal(result.getTotal());
        vo.setCurrent(result.getCurrent());
        vo.setSize(result.getSize());
        return vo;
    }

    @Override
    public AdminVideoVo detail(Long vid) {
        if (vid == null) {
            return null;
        }
        Video video = videoMapper.selectById(vid);
        if (video == null) {
            return null;
        }
        return toVo(video, nicknameOf(userBriefService.byUid(video.getUid())));
    }

    @Override
    public void updateStatus(Long vid, Integer status) {
        if (status == null) {
            throw new VideoException(202, "缺少目标状态");
        }
        if (videoMapper.selectById(vid) == null) {
            throw new VideoException(404, "稿件不存在");
        }
        // 下架写删除时间，其余状态（过审/打回）清空删除时间
        Date deleteDate = status == STATUS_DELETED ? new Date() : null;
        videoMapper.updateAdminStatus(vid, status, deleteDate);
        log.info("admin contract action=video.status vid={} to={}", vid, status);
    }

    @Override
    public void updateVisible(Long vid, Integer visible) {
        if (visible == null || (visible != 0 && visible != 1)) {
            throw new VideoException(202, "visible 只能是 0 或 1");
        }
        if (videoMapper.selectById(vid) == null) {
            throw new VideoException(404, "稿件不存在");
        }
        videoMapper.updateAdminVisible(vid, visible);
    }

    @Override
    public List<AdminVideoRefVo> refsByUrl(String url) {
        if (url == null || url.isBlank()) {
            return List.of();
        }
        return videoMapper.findVideoRefs(url);
    }

    @Override
    public AdminVideoRefVo hlsRef(Long vid) {
        return vid == null ? null : videoMapper.findHlsRef(vid);
    }

    @Override
    public List<AdminCoverThumbVo> missingThumbs(long afterVid, int limit) {
        // 分页大小由调用方控制，这里只兜底非法上限
        int safeLimit = Math.min(200, Math.max(1, limit));
        return videoMapper.listMissingThumbs(Math.max(0L, afterVid), safeLimit);
    }

    @Override
    public int updateThumb(Long vid, String thumbUrl) {
        if (thumbUrl == null || thumbUrl.isBlank()) {
            throw new VideoException(202, "缩略图地址不能为空");
        }
        return videoMapper.updateCoverThumb(vid, thumbUrl);
    }

    /** 作者昵称：用户域不可用时降级为空，不阻断管理列表 */
    private String nicknameOf(Map<Long, UserBriefVo> authors, Long uid) {
        UserBriefVo author = authors.get(uid);
        return author == null ? null : author.getNickname();
    }

    private String nicknameOf(UserBriefVo author) {
        return author == null ? null : author.getNickname();
    }

    /** Video → 管理端视图，字段与 AL-admin 对外返回保持一致 */
    private AdminVideoVo toVo(Video video, String authorName) {
        AdminVideoVo vo = new AdminVideoVo();
        vo.setVid(video.getVid());
        vo.setUid(video.getUid());
        vo.setTitle(video.getTitle());
        vo.setStatus(video.getStatus());
        vo.setVisible(video.getVisible());
        vo.setTranscodeStatus(video.getTranscodeStatus());
        vo.setCoverUrl(video.getCoverUrl());
        vo.setVideoUrl(video.getVideoUrl());
        vo.setHlsUrl(video.getHlsUrl());
        vo.setAuthorName(authorName);
        vo.setUploadDate(video.getUploadDate());
        vo.setDeleteDate(video.getDeleteDate());
        return vo;
    }
}
