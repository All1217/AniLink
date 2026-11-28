package com.video.admin;

import com.video.admin.service.impl.AdminVideoServiceImpl;
import com.video.api.video.AdminVideoInternalClient;
import com.video.common.exception.VideoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 稿件状态流转与可见性规则；稿件读写改走 al-video 内部契约。 */
@ExtendWith(MockitoExtension.class)
class AdminVideoTest {

    @Mock
    private AdminVideoInternalClient adminVideoClient;
    @InjectMocks
    private AdminVideoServiceImpl service;

    @Test
    void approvePendingVideo() {
        when(adminVideoClient.detail(1L)).thenReturn(video(1L, 0));
        service.updateStatus(1L, 1, 9L);
        verify(adminVideoClient).updateStatus(1L, 1);
    }

    @Test
    void rejectPendingVideo() {
        when(adminVideoClient.detail(2L)).thenReturn(video(2L, 0));
        service.updateStatus(2L, 2, 9L);
        verify(adminVideoClient).updateStatus(2L, 2);
    }

    @Test
    void takedownWritesDeleteDate() {
        // 下架的 delete_date 由视频域契约写入，管理端只负责校验流转并透传目标状态
        when(adminVideoClient.detail(3L)).thenReturn(video(3L, 1));
        service.updateStatus(3L, 3, 9L);
        verify(adminVideoClient).updateStatus(3L, 3);
    }

    @Test
    void repeatedApproveIsRejected() {
        when(adminVideoClient.detail(4L)).thenReturn(video(4L, 1));
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateStatus(4L, 1, 9L));
        assertEquals(409, error.getCode());
        verify(adminVideoClient, never()).updateStatus(any(), any());
    }

    @Test
    void invalidVisibilityRejected() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.updateVisible(5L, 2, 9L));
        assertEquals(202, error.getCode());
        verify(adminVideoClient, never()).updateVisible(any(), any());
    }

    @Test
    void visibilityUpdates() {
        when(adminVideoClient.detail(6L)).thenReturn(video(6L, 1));
        service.updateVisible(6L, 1, 9L);
        verify(adminVideoClient).updateVisible(6L, 1);
    }

    @Test
    void detailMissingReturns404() {
        when(adminVideoClient.detail(7L)).thenReturn(null);
        VideoException error = assertThrows(VideoException.class, () -> service.detail(7L));
        assertEquals(404, error.getCode());
    }

    private com.video.api.video.dto.AdminVideoVo video(Long vid, int status) {
        com.video.api.video.dto.AdminVideoVo video = new com.video.api.video.dto.AdminVideoVo();
        video.setVid(vid);
        video.setStatus(status);
        return video;
    }
}
