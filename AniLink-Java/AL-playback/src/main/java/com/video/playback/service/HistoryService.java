package com.video.playback.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.video.model.dto.web.VideoRecordFormDTO;
import com.video.model.entity.BrowseHistory;
import com.video.playback.vo.HistoryQueryVo;
import com.video.playback.vo.HistoryVo;

/** 观看历史与播放进度：历史增删查、进度合并后延迟落库。 */
public interface HistoryService extends IService<BrowseHistory> {
    void insertOne(BrowseHistory browseHistory);

    IPage<HistoryVo> getHistoryPage(IPage<HistoryVo> page, HistoryQueryVo queryVo);

    void deleteByVids(HistoryQueryVo queryVo);

    void deleteSingle(HistoryQueryVo queryVo);

    void deleteByUid(HistoryQueryVo queryVo);

    boolean addVideoRecord(VideoRecordFormDTO formDTO);
}
