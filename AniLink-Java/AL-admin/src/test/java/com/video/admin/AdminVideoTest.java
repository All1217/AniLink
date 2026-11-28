package com.video.admin;

import com.video.common.exception.VideoException;
import com.video.model.entity.Video;
import com.video.admin.mapper.AdminVideoMapper;
import com.video.admin.service.impl.AdminVideoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 稿件状态流转与可见性规则。 */
@ExtendWith(MockitoExtension.class)
class AdminVideoTest {

    @Mock
    private AdminVideoMapper adminVideoMapper;
    @InjectMocks
    private AdminVideoServiceImpl service;

    @Test
    void approvePendingVideo() {
        when(adminVideoMapper.selectVideoStatus(1L)).thenReturn(video(1L, 0));
        service.updateStatus(1L, 1, 9L);
        verify(adminVideoMapper).updateStatus(eq(1L), eq(1), isNull());
    }

    @Test
    void rejectPendingVideo() {
        when(adminVideoMapper.selectVideoStatus(2L)).thenReturn(video(2L, 0));
        service.updateStatus(2L, 2, 9L);
        verify(adminVideoMapper).updateStatus(eq(2L), eq(2), isNull());
    }

    @Test
    void takedownWritesDeleteDate() {
        when(adminVideoMapper.selectVideoStatus(3L)).thenReturn(video(3L, 1));
        service.updateStatus(3L, 3, 9L);
        verify(adminVideoMapper).updateStatus(eq(3L), eq(3), any());
    }

    @Test
    void repeatedApproveIsRejected() {
        when(adminVideoMapper.selectVideoStatus(4L)).thenReturn(video(4L, 1));
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateStatus(4L, 1, 9L));
        assertEquals(409, error.getCode());
        verify(adminVideoMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void invalidVisibilityRejected() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateVisible(5L, 2, 9L));
        assertEquals(202, error.getCode());
        verify(adminVideoMapper, never()).updateVisible(any(), any());
    }

    @Test
    void visibilityUpdates() {
        when(adminVideoMapper.selectVideoStatus(6L)).thenReturn(video(6L, 1));
        service.updateVisible(6L, 1, 9L);
        verify(adminVideoMapper).updateVisible(6L, 1);
    }

    @Test
    void detailMissingReturns404() {
        when(adminVideoMapper.getAdminDetail(7L)).thenReturn(null);
        VideoException error = assertThrows(VideoException.class, () -> service.detail(7L));
        assertEquals(404, error.getCode());
    }

    private Video video(Long vid, int status) {
        Video video = new Video();
        video.setVid(vid);
        video.setStatus(status);
        return video;
    }
}
