package com.video.video.service;

import com.video.model.dto.social.FavoriteChangedDTO;
import com.video.video.mapper.UserVideoMapper;
import com.video.video.mapper.VideoStatsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 收藏事件落库：收藏标记覆盖 + 视频收藏数按增量更新。 */
@ExtendWith(MockitoExtension.class)
class FavoriteCollectServiceTest {

    @Mock
    private UserVideoMapper userVideoMapper;
    @Mock
    private VideoStatsMapper videoStatsMapper;
    @InjectMocks
    private FavoriteCollectService service;

    @Test
    void collectEventWritesFlagAndIncreasesCount() {
        when(videoStatsMapper.addCollect(35L, 1)).thenReturn(1);

        service.apply(event(1, 1));

        verify(userVideoMapper).upsertCollect(eq(7L), eq(35L), eq(1), any(Date.class));
        verify(videoStatsMapper).addCollect(35L, 1);
    }

    @Test
    void unCollectEventClearsFlagAndDecreasesCount() {
        when(videoStatsMapper.addCollect(35L, -1)).thenReturn(1);

        service.apply(event(0, -1));

        // 取消收藏不清空收藏时间
        verify(userVideoMapper).upsertCollect(eq(7L), eq(35L), eq(0), isNull());
        verify(videoStatsMapper).addCollect(35L, -1);
    }

    /** 构造一条收藏变更事件 */
    private FavoriteChangedDTO event(int collected, int delta) {
        FavoriteChangedDTO event = new FavoriteChangedDTO();
        event.setEventId("e-" + collected + "-" + delta);
        event.setUid(7L);
        event.setVid(35L);
        event.setCollected(collected);
        event.setDelta(delta);
        event.setOccurredAt(System.currentTimeMillis());
        return event;
    }
}
