package com.video.playback.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.video.common.result.Result;
import com.video.model.entity.BrowseHistory;
import com.video.playback.service.HistoryService;
import com.video.playback.vo.HistoryQueryVo;
import com.video.playback.vo.HistoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 历史记录相关接口（路径对外保持 /main/history/** 不变）。 */
@Tag(name = "历史记录相关接口")
@RestController
@RequestMapping("/main/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "新增历史记录")
    @PostMapping("/add")
    public Result insetOne(@RequestBody BrowseHistory browseHistory) {
        historyService.insertOne(browseHistory);
        return Result.ok();
    }

    @Operation(summary = "分页查找历史记录")
    @GetMapping("/getHistory/page")
    public Result<IPage<HistoryVo>> getHistoryPage(@RequestParam long current, @RequestParam long size,
                                                  HistoryQueryVo queryVo) {
        IPage<HistoryVo> page = new Page<>(current, size);
        IPage<HistoryVo> result = historyService.getHistoryPage(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "删除一条历史记录")
    @DeleteMapping("/deleteSingle")
    public Result deleteSingle(@RequestBody HistoryQueryVo queryVo) {
        historyService.deleteSingle(queryVo);
        return Result.ok();
    }

    @Operation(summary = "删除多条历史记录")
    @DeleteMapping("/deleteByVids")
    public Result deleteByVids(@RequestBody HistoryQueryVo queryVo) {
        historyService.deleteByVids(queryVo);
        return Result.ok();
    }

    @Operation(summary = "清空历史记录")
    @DeleteMapping("/deleteByUid")
    public Result deleteByUid(@RequestBody HistoryQueryVo queryVo) {
        historyService.deleteByUid(queryVo);
        return Result.ok();
    }
}
