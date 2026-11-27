package com.video.playback.controller;

import com.video.model.dto.web.VideoRecordFormDTO;
import com.video.playback.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 播放进度上报：新路径 /main/playback/record/add，旧路径 /main/video/interact/record/add 保留一个发布周期。 */
@Tag(name = "播放进度相关接口")
@RestController
@RequiredArgsConstructor
public class PlaybackController {

    private final HistoryService historyService;

    @Operation(summary = "上报播放进度（新路径与旧路径等价）")
    @PostMapping({"/main/playback/record/add", "/main/video/interact/record/add"})
    public void addVideoRecord(@RequestBody VideoRecordFormDTO formDTO) {
        historyService.addVideoRecord(formDTO);
    }
}
