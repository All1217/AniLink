package com.video.danmaku.service.impl;

import com.video.danmaku.mapper.DanmuMapper;
import com.video.model.entity.Danmu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 弹幕域单测：发送弹幕时主键归零交给数据库自增，保持迁移前落库语义。 */
@ExtendWith(MockitoExtension.class)
class DanmuServiceImplTest {

    @Mock
    private DanmuMapper danmuMapper;
    @InjectMocks
    private DanmuServiceImpl danmuService;

    @Test
    @DisplayName("发送弹幕把 id 置 0 后落库")
    void insertOneForcesAutoIncrementId() {
        Danmu danmu = new Danmu();
        danmu.setId(999L);
        danmu.setVid(7L);
        danmu.setUid(42L);
        danmu.setContent("前方高能");
        when(danmuMapper.insert(any())).thenReturn(1);

        danmuService.insertOne(danmu);

        ArgumentCaptor<Danmu> captor = ArgumentCaptor.forClass(Danmu.class);
        verify(danmuMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getId());
        assertEquals(7L, captor.getValue().getVid());
    }
}
