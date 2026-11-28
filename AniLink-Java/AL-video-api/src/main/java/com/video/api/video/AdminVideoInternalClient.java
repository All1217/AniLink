package com.video.api.video;

import com.video.api.video.dto.AdminCoverThumbVo;
import com.video.api.video.dto.AdminVideoPageVo;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.api.video.dto.AdminVideoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 视频域管理契约：只给 AL-admin 调用，网关不暴露 /internal/**。
 * 视频表归 al-video 所有，管理端的审核/可见性/存储引用/封面缩略图回填都经这里，禁止跨库直连。
 */
@FeignClient(name = "videoservice", contextId = "adminVideoInternalClient",
        url = "${al.video.service-url:}", path = "/internal/v1/admin/videos")
public interface AdminVideoInternalClient {

    /** 稿件分页：不传 status 时默认排除已删除（status=3），visible/uid/keyword 为可选过滤 */
    @GetMapping
    AdminVideoPageVo page(@RequestParam("current") long current,
                          @RequestParam("size") long size,
                          @RequestParam(value = "status", required = false) Integer status,
                          @RequestParam(value = "visible", required = false) Integer visible,
                          @RequestParam(value = "uid", required = false) Long uid,
                          @RequestParam(value = "keyword", required = false) String keyword);

    /** 稿件详情（含作者昵称）；不存在返回 null */
    @GetMapping("/{vid}")
    AdminVideoVo detail(@PathVariable("vid") Long vid);

    /** 更新审核/删除状态；目标为 3 时写 delete_date，其余清空 */
    @PutMapping("/{vid}/status")
    void updateStatus(@PathVariable("vid") Long vid, @RequestParam("status") Integer status);

    /** 更新公开/仅自己可见 */
    @PutMapping("/{vid}/visible")
    void updateVisible(@PathVariable("vid") Long vid, @RequestParam("visible") Integer visible);

    /** 按完整 URL 查稿件字段引用，存储对象删除前校验用 */
    @GetMapping("/refs")
    List<AdminVideoRefVo> refs(@RequestParam("url") String url);

    /** HLS 目录引用：该 vid 仍有 hls_url 就视为整个 hls/{vid} 目录被引用 */
    @GetMapping("/{vid}/hls-ref")
    AdminVideoRefVo hlsRef(@PathVariable("vid") Long vid);

    /** 按 vid 游标取缺少封面缩略图的稿件，天然幂等 */
    @GetMapping("/cover-thumbs/missing")
    List<AdminCoverThumbVo> missingThumbs(@RequestParam("afterVid") long afterVid,
                                          @RequestParam("limit") int limit);

    /** 写回封面缩略图，仅当前值为空时生效；返回更新行数 */
    @PutMapping("/cover-thumbs/{vid}")
    int updateThumb(@PathVariable("vid") Long vid, @RequestParam("thumbUrl") String thumbUrl);
}
