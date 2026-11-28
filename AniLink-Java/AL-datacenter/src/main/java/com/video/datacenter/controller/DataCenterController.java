package com.video.datacenter.controller;

import com.video.common.result.Result;
import com.video.datacenter.service.DataCenterService;
import com.video.datacenter.vo.DataResVo;
import com.video.model.entity.HotSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 数据中心接口：路径与拆分前 /main/dataCenter/** 保持一致，网关按该前缀切到 datacenterservice。 */
@Tag(name = "数据分析相关接口")
@RestController
@RequestMapping("/main/dataCenter")
public class DataCenterController {

    private final DataCenterService dataCenterService;

    public DataCenterController(DataCenterService dataCenterService) {
        this.dataCenterService = dataCenterService;
    }

    @Operation(summary = "查询总播放量")
    @GetMapping("/total/get/play")
    public Result<DataResVo> getTotalPlay(@RequestParam Long uid,
                                          @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                          @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.ok(dataCenterService.getTotalPlay(uid, begin, end));
    }

    @Operation(summary = "查询总点赞数")
    @GetMapping("/total/get/like")
    public Result<DataResVo> getLike(@RequestParam Long uid,
                                     @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                     @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.ok(dataCenterService.getLike(uid, begin, end));
    }

    @Operation(summary = "获取热搜列表")
    @GetMapping("/mainWindow/get/hotSearch")
    public Result<List<HotSearch>> getHotSearch() {
        return Result.ok(dataCenterService.getHotSearch());
    }
}
