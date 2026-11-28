package com.video.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.common.login.LoginUserHolder;
import com.video.common.result.Result;
import com.video.admin.service.AdminVideoService;
import com.video.admin.vo.AdminVideoQueryVo;
import com.video.admin.vo.AdminVideoStatusVo;
import com.video.admin.vo.AdminVideoVisibilityVo;
import com.video.admin.vo.AdminVideoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端视频稿件接口。 */
@Tag(name = "管理端-视频稿件")
@RestController
@RequestMapping("/main/admin/videos")
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

    public AdminVideoController(AdminVideoService adminVideoService) {
        this.adminVideoService = adminVideoService;
    }

    @Operation(summary = "稿件分页")
    @GetMapping
    public Result<IPage<AdminVideoVo>> page(AdminVideoQueryVo query) {
        return Result.ok(adminVideoService.page(query));
    }

    @Operation(summary = "稿件详情")
    @GetMapping("/{vid}")
    public Result<AdminVideoVo> detail(@PathVariable Long vid) {
        return Result.ok(adminVideoService.detail(vid));
    }

    @Operation(summary = "审核/下架稿件")
    @PutMapping("/{vid}/status")
    public Result<Void> updateStatus(@PathVariable Long vid, @RequestBody AdminVideoStatusVo body) {
        adminVideoService.updateStatus(vid, body.getStatus(), operatorUid());
        return Result.ok();
    }

    @Operation(summary = "修改稿件可见性")
    @PutMapping("/{vid}/visible")
    public Result<Void> updateVisible(@PathVariable Long vid, @RequestBody AdminVideoVisibilityVo body) {
        adminVideoService.updateVisible(vid, body.getVisible(), operatorUid());
        return Result.ok();
    }

    private Long operatorUid() {
        return LoginUserHolder.getLoginUser().getUserId();
    }
}
