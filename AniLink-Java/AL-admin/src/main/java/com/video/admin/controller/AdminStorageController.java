package com.video.admin.controller;

import com.video.common.login.LoginUserHolder;
import com.video.common.result.Result;
import com.video.admin.service.AdminStorageService;
import com.video.admin.vo.AdminBackfillResultVo;
import com.video.admin.vo.AdminCleanupResultVo;
import com.video.admin.vo.AdminObjectPageVo;
import com.video.admin.vo.AdminRefVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端对象存储接口。 */
@Tag(name = "管理端-对象存储")
@RestController
@RequestMapping("/main/admin/storage")
public class AdminStorageController {

    private final AdminStorageService adminStorageService;

    public AdminStorageController(AdminStorageService adminStorageService) {
        this.adminStorageService = adminStorageService;
    }

    @Operation(summary = "允许的桶列表")
    @GetMapping("/buckets")
    public Result<List<String>> buckets() {
        return Result.ok(adminStorageService.listBuckets());
    }

    @Operation(summary = "对象分页")
    @GetMapping("/objects")
    public Result<AdminObjectPageVo> objects(@RequestParam String bucket,
                                             @RequestParam(required = false) String prefix,
                                             @RequestParam(required = false) String cursor,
                                             @RequestParam(defaultValue = "50") int size) {
        return Result.ok(adminStorageService.listObjects(bucket, prefix, cursor, size));
    }

    @Operation(summary = "对象预签名预览地址")
    @GetMapping("/objects/presign")
    public Result<String> presign(@RequestParam String bucket, @RequestParam String key) {
        return Result.ok(adminStorageService.presign(bucket, key));
    }

    @Operation(summary = "删除对象（先做业务引用检查）")
    @DeleteMapping("/objects")
    public Result<Object> delete(@RequestParam String bucket, @RequestParam String key) {
        List<AdminRefVo> refs = adminStorageService.delete(bucket, key, operatorUid());
        if (!refs.isEmpty()) {
            Result<Object> result = Result.fail(409, "对象被业务引用，未删除");
            result.setData(refs);
            return result;
        }
        return Result.ok();
    }

    @Operation(summary = "一键清除当前桶/前缀下无引用对象")
    @PostMapping("/cleanup")
    public Result<AdminCleanupResultVo> cleanup(@RequestParam String bucket,
                                                @RequestParam(required = false) String prefix) {
        return Result.ok(adminStorageService.cleanup(bucket, prefix, operatorUid()));
    }

    @Operation(summary = "重建存量封面缩略图")
    @PostMapping("/cover-thumbs/backfill")
    public Result<AdminBackfillResultVo> backfillCoverThumbs() {
        return Result.ok(adminStorageService.backfillCoverThumbs(operatorUid()));
    }

    private Long operatorUid() {
        return LoginUserHolder.getLoginUser().getUserId();
    }
}
