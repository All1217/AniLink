package com.video.admin;

import com.video.admin.service.ImageProcessService;
import com.video.admin.service.impl.AdminStorageServiceImpl;
import com.video.admin.vo.AdminCleanupResultVo;
import com.video.admin.vo.AdminObjectPageVo;
import com.video.admin.vo.AdminObjectVo;
import com.video.admin.vo.AdminRefVo;
import com.video.api.user.AdminUserInternalClient;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.api.video.AdminVideoInternalClient;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.common.exception.VideoException;
import com.video.common.properties.MinIOProperties;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对象存储白名单与删除前引用检查；引用改由用户域/视频域内部契约提供。 */
@ExtendWith(MockitoExtension.class)
class AdminStorageTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinIOProperties minIOProperties;
    @Mock
    private AdminVideoInternalClient adminVideoClient;
    @Mock
    private AdminUserInternalClient adminUserClient;
    @Mock
    private ImageProcessService imageProcessService;
    @InjectMocks
    private AdminStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(minIOProperties.getEndpoint()).thenReturn("http://localhost:9000");
    }

    @Test
    void invalidBucketRejected() {
        VideoException error = assertThrows(VideoException.class,
                () -> service.listObjects("other", "", null, 10));
        assertEquals(202, error.getCode());
    }

    @Test
    void referencedObjectIsNotDeleted() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of(videoRef(1L, "video_url")));
        when(adminUserClient.refs(anyString())).thenReturn(List.of());

        List<AdminRefVo> refs = service.delete("video", "a.mp4", 9L);

        assertEquals(1, refs.size());
        verify(minioClient, never()).removeObject(any());
    }

    @Test
    void referencedUserAvatarIsNotDeleted() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of());
        when(adminUserClient.refs(anyString())).thenReturn(List.of(userRef(2L, "avatar")));

        List<AdminRefVo> refs = service.delete("avatar", "a.jpg", 9L);

        assertEquals(1, refs.size());
        verify(minioClient, never()).removeObject(any());
    }

    @Test
    void hlsSegmentIsBlockedByHlsUrl() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of());
        when(adminUserClient.refs(anyString())).thenReturn(List.of());
        when(adminVideoClient.hlsRef(35L)).thenReturn(videoRef(35L, "hls_url"));

        List<AdminRefVo> refs = service.delete("video", "hls/35/seg.m4s", 9L);

        assertEquals(1, refs.size());
        verify(minioClient, never()).removeObject(any());
    }

    @Test
    void unreferencedObjectIsDeleted() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of());
        when(adminUserClient.refs(anyString())).thenReturn(List.of());

        List<AdminRefVo> refs = service.delete("video-cover", "cover.jpg", 9L);

        assertEquals(0, refs.size());
        verify(minioClient).removeObject(any());
    }

    @Test
    void presignReturnsShortLivedUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any())).thenReturn("http://localhost:9000/video/a.mp4?sign=1");

        assertEquals("http://localhost:9000/video/a.mp4?sign=1",
                service.presign("video", "a.mp4"));
    }

    @Test
    void deleteFailureSurfacesAs500() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of());
        when(adminUserClient.refs(anyString())).thenReturn(List.of());
        doThrow(new java.io.IOException("minio down")).when(minioClient).removeObject(any());

        VideoException error = assertThrows(VideoException.class,
                () -> service.delete("video-cover", "cover.jpg", 9L));

        assertEquals(500, error.getCode());
    }

    @Test
    void cleanupDeletesOnlyUnreferencedObjects() throws Exception {
        AdminObjectVo used = object("used.mp4");
        AdminObjectVo orphan = object("orphan.mp4");
        AdminObjectPageVo page = new AdminObjectPageVo();
        page.setItems(List.of(used, orphan));
        page.setHasMore(false);

        AdminStorageServiceImpl spyService = spy(service);
        doReturn(page).when(spyService).listObjects("video", "", null, 200);
        when(adminVideoClient.refs(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            return url.endsWith("/used.mp4") ? List.of(videoRef(1L, "video_url")) : List.of();
        });
        when(adminUserClient.refs(anyString())).thenReturn(List.of());

        AdminCleanupResultVo result = spyService.cleanup("video", "", 9L);

        assertEquals(2, result.getScanned());
        assertEquals(1, result.getDeleted());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getFailed());
        verify(minioClient).removeObject(any());
    }

    @Test
    void referencedCoverThumbIsNotDeleted() throws Exception {
        when(adminVideoClient.refs(anyString())).thenReturn(List.of(videoRef(3L, "cover_thumb_url")));
        when(adminUserClient.refs(anyString())).thenReturn(List.of());

        List<AdminRefVo> refs = service.delete("video-cover", "a_480.webp", 9L);

        assertEquals(1, refs.size());
        verify(minioClient, never()).removeObject(any());
    }

    private AdminObjectVo object(String key) {
        AdminObjectVo vo = new AdminObjectVo();
        vo.setBucket("video");
        vo.setKey(key);
        return vo;
    }

    private AdminVideoRefVo videoRef(Long id, String field) {
        AdminVideoRefVo ref = new AdminVideoRefVo();
        ref.setType("video");
        ref.setId(id);
        ref.setField(field);
        return ref;
    }

    private AdminUserRefVo userRef(Long id, String field) {
        AdminUserRefVo ref = new AdminUserRefVo();
        ref.setType("user");
        ref.setId(id);
        ref.setField(field);
        return ref;
    }
}
