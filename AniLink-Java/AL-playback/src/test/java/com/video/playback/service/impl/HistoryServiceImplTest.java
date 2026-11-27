package com.video.playback.service.impl;

import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.model.dto.web.VideoRecordFormDTO;
import com.video.model.entity.BrowseHistory;
import com.video.playback.mapper.HistoryMapper;
import com.video.playback.service.UserBriefService;
import com.video.playback.service.VideoBriefService;
import com.video.playback.task.VideoRecordCache;
import com.video.playback.task.VideoRecordDelayQueue;
import com.video.playback.vo.HistoryQueryVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 播放进度上报单测：首次直接落库、并发首插撞唯一键降级更新、已有记录进延迟队列。 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    private static final long VID = 7L;
    private static final long UID = 42L;

    @Mock
    private HistoryMapper historyMapper;
    @Mock
    private VideoRecordCache recordCache;
    @Mock
    private VideoRecordDelayQueue delayQueue;
    @Mock
    private UserBriefService userBriefService;
    @Mock
    private VideoBriefService videoBriefService;

    private HistoryServiceImpl historyService;

    @BeforeEach
    void setUp() {
        historyService = new HistoryServiceImpl(historyMapper, recordCache, delayQueue,
                userBriefService, videoBriefService);
        LoginUserHolder.setLoginUser(new LoginUser(UID, "tester"));
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
    }

    @Test
    @DisplayName("首次上报：直接插入 bh_video，不进入延迟队列")
    void firstReportInsertsDirectly() {
        when(recordCache.read(VID, UID)).thenReturn(null);
        when(historyMapper.selectOne(any())).thenReturn(null);
        when(historyMapper.insert(any(BrowseHistory.class))).thenReturn(1);

        boolean updated = historyService.addVideoRecord(form(30.0));

        assertFalse(updated, "首次上报返回 false，与旧实现语义一致");
        verify(historyMapper).insert(any(BrowseHistory.class));
        verify(delayQueue, never()).submit(any());
    }

    @Test
    @DisplayName("并发首插撞唯一键：降级为更新并进入延迟落库，不产生重复行")
    void duplicateKeyFallsBackToUpdate() {
        BrowseHistory exists = new BrowseHistory();
        exists.setId(100L);
        when(recordCache.read(VID, UID)).thenReturn(null);
        // 第一次查旧记录为空；插入撞唯一键后再查一次，拿到并发请求已写入的记录
        when(historyMapper.selectOne(any())).thenReturn(null, exists);
        when(historyMapper.insert(any(BrowseHistory.class)))
                .thenThrow(new DuplicateKeyException("uniq_uid_vid"));

        boolean updated = historyService.addVideoRecord(form(45.0));

        assertTrue(updated);
        // 插入被唯一键拦下后没有再尝试第二次插入，而是复用已存在记录的主键做更新
        verify(historyMapper).insert(any(BrowseHistory.class));
        ArgumentCaptor<BrowseHistory> recordCaptor = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(delayQueue).submit(recordCaptor.capture());
        assertEquals(100L, recordCaptor.getValue().getId().longValue(),
                "降级更新应复用并发请求写入的主键");
        assertEquals(45.0, recordCaptor.getValue().getDuration(), 0.0);
    }

    @Test
    @DisplayName("已有记录：只写读缓存与延迟队列，不直接更新数据库")
    void existingRecordGoesToDelayQueue() {
        BrowseHistory cached = new BrowseHistory();
        cached.setId(100L);
        cached.setVid(VID);
        cached.setUid(UID);
        cached.setDuration(20.0);
        when(recordCache.read(VID, UID)).thenReturn(cached);

        boolean updated = historyService.addVideoRecord(form(60.0));

        assertTrue(updated);
        verify(historyMapper, never()).insert(any(BrowseHistory.class));
        verify(historyMapper, never()).updateById(any(BrowseHistory.class));
        ArgumentCaptor<BrowseHistory> recordCaptor = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(delayQueue).submit(recordCaptor.capture());
        assertEquals(60.0, recordCaptor.getValue().getDuration(), 0.0);
    }

    @Test
    @DisplayName("删除单条历史：删库后同步清理读缓存与待落库任务")
    void deleteSingleCleansCacheAndDelayTask() {
        HistoryQueryVo queryVo = new HistoryQueryVo();
        queryVo.setVids(List.of(VID));

        historyService.deleteSingle(queryVo);

        verify(historyMapper).delete(UID, VID);
        verify(recordCache).clean(VID, UID);
        verify(delayQueue).remove(VID, UID);
    }

    @Test
    @DisplayName("清空历史：按用户全部 vid 逐条清理缓存与待落库任务")
    void deleteByUidCleansAllCaches() {
        HistoryQueryVo queryVo = new HistoryQueryVo();
        when(historyMapper.listVidsByUid(UID)).thenReturn(List.of(7L, 8L));

        historyService.deleteByUid(queryVo);

        verify(historyMapper).deleteByUid(UID);
        verify(recordCache).clean(7L, UID);
        verify(recordCache).clean(8L, UID);
        verify(delayQueue).remove(7L, UID);
        verify(delayQueue).remove(8L, UID);
    }

    @Test
    @DisplayName("新增历史接口：记录已存在时只刷新观看时间，等价旧 upsert 语义")
    void insertOneFallsBackToViewTimeUpdate() {
        BrowseHistory incoming = new BrowseHistory();
        incoming.setVid(VID);
        incoming.setViewTime(new java.util.Date());
        BrowseHistory exists = new BrowseHistory();
        exists.setId(100L);
        when(historyMapper.insert(any(BrowseHistory.class)))
                .thenThrow(new DuplicateKeyException("uniq_uid_vid"));
        when(historyMapper.selectOne(any())).thenReturn(exists);

        historyService.insertOne(incoming);

        ArgumentCaptor<BrowseHistory> recordCaptor = ArgumentCaptor.forClass(BrowseHistory.class);
        verify(historyMapper).updateById(recordCaptor.capture());
        assertEquals(100L, recordCaptor.getValue().getId().longValue());
        assertEquals(incoming.getViewTime(), recordCaptor.getValue().getViewTime());
    }

    /** 构造播放进度上报表单 */
    private VideoRecordFormDTO form(double moment) {
        VideoRecordFormDTO formDTO = new VideoRecordFormDTO();
        formDTO.setVid(VID);
        formDTO.setDuration(180);
        formDTO.setMoment(moment);
        formDTO.setCommitTime(LocalDateTime.now());
        return formDTO;
    }
}
