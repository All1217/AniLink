package com.video.video.controller.internal;

import com.video.api.video.dto.AdminCoverThumbVo;
import com.video.api.video.dto.AdminVideoPageVo;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.api.video.dto.AdminVideoVo;
import com.video.video.service.AdminVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 视频域管理契约实现，只给 AL-admin 调用（网关不暴露 /internal/**）。
 * 返回裸 DTO 而不是 Result，便于 Feign 直接反序列化。
 */
@Tag(name = "内部-管理视频")
@RestController
@RequestMapping("/internal/v1/admin/videos")
public class InternalAdminVideoController {

    private final AdminVideoService adminVideoService;

    public InternalAdminVideoController(AdminVideoService adminVideoService) {
        this.adminVideoService = adminVideoService;
    }

    @Operation(summary = "管理端稿件分页")
    @GetMapping
    public AdminVideoPageVo page(@RequestParam(value = "current", defaultValue = "1") long current,
                                 @RequestParam(value = "size", defaultValue = "10") long size,
                                 @RequestParam(value = "status", required = false) Integer status,
                                 @RequestParam(value = "visible", required = false) Integer visible,
                                 @RequestParam(value = "uid", required = false) Long uid,
                                 @RequestParam(value = "keyword", required = false) String keyword) {
        return adminVideoService.page(current, size, status, visible, uid, keyword);
    }

    @Operation(summary = "管理端稿件详情（不存在返回 null）")
    @GetMapping("/{vid}")
    public AdminVideoVo detail(@PathVariable("vid") Long vid) {
        return adminVideoService.detail(vid);
    }

    @Operation(summary = "更新审核/删除状态")
    @PutMapping("/{vid}/status")
    public void updateStatus(@PathVariable("vid") Long vid, @RequestParam("status") Integer status) {
        adminVideoService.updateStatus(vid, status);
    }

    @Operation(summary = "更新稿件可见性")
    @PutMapping("/{vid}/visible")
    public void updateVisible(@PathVariable("vid") Long vid, @RequestParam("visible") Integer visible) {
        adminVideoService.updateVisible(vid, visible);
    }

    @Operation(summary = "按完整 URL 查稿件字段引用")
    @GetMapping("/refs")
    public List<AdminVideoRefVo> refs(@RequestParam("url") String url) {
        return adminVideoService.refsByUrl(url);
    }

    @Operation(summary = "HLS 目录引用（不存在返回 null）")
    @GetMapping("/{vid}/hls-ref")
    public AdminVideoRefVo hlsRef(@PathVariable("vid") Long vid) {
        return adminVideoService.hlsRef(vid);
    }

    @Operation(summary = "取缺少封面缩略图的稿件")
    @GetMapping("/cover-thumbs/missing")
    public List<AdminCoverThumbVo> missingThumbs(@RequestParam("afterVid") long afterVid,
                                                 @RequestParam("limit") int limit) {
        return adminVideoService.missingThumbs(afterVid, limit);
    }

    @Operation(summary = "写回封面缩略图（仅空值生效）")
    @PutMapping("/cover-thumbs/{vid}")
    public int updateThumb(@PathVariable("vid") Long vid, @RequestParam("thumbUrl") String thumbUrl) {
        return adminVideoService.updateThumb(vid, thumbUrl);
    }
}
