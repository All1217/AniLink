package com.video.web.main.service;

import com.video.common.exception.VideoException;
import com.video.common.properties.MinIOProperties;
import com.video.common.result.Result;
import com.video.web.main.config.ImageProperties;
import com.video.web.main.service.impl.FileServiceImpl;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 封面上传：派生、校验与失败回退。 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinIOProperties minIOProperties;
    @Mock
    private ImageProcessService imageProcessService;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        lenient().when(minIOProperties.getEndpoint()).thenReturn("http://localhost:9000");
        fileService = new FileServiceImpl(minioClient, minIOProperties, new ImageProperties(), imageProcessService);
    }

    @Test
    void coverUploadsTwoWebpVariants() throws Exception {
        when(imageProcessService.coverLarge(any(), anyString())).thenReturn(new byte[]{1, 2, 3});
        when(imageProcessService.coverThumb(any(), anyString())).thenReturn(new byte[]{4});

        Result result = fileService.uploadVideoCover(image("a.jpg", "image/jpeg", 1024));

        assertEquals(200, result.getCode());
        assertTrue(((String) result.getData()).endsWith("_960.webp"));
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient, times(2)).putObject(captor.capture());
        List<String> objects = captor.getAllValues().stream().map(PutObjectArgs::object).toList();
        assertTrue(objects.stream().anyMatch(name -> name.endsWith("_480.webp")));
        assertTrue(objects.stream().anyMatch(name -> name.endsWith("_960.webp")));
    }

    @Test
    void avatarUploadsSquareWebp() throws Exception {
        when(imageProcessService.avatar(any(), anyString())).thenReturn(new byte[]{7});

        Result result = fileService.uploadAvatar(image("a.png", "image/png", 1024));

        assertEquals(200, result.getCode());
        assertTrue(((String) result.getData()).endsWith("_192.webp"));
        verify(minioClient, times(1)).putObject(any());
    }

    @Test
    void deriveFailureFallsBackToOriginal() throws Exception {
        when(imageProcessService.coverLarge(any(), anyString())).thenThrow(new IOException("boom"));

        Result result = fileService.uploadVideoCover(image("a.jpg", "image/jpeg", 1024));

        assertEquals(200, result.getCode());
        String url = (String) result.getData();
        assertTrue(url.endsWith(".jpg"), "派生失败应回退原图地址：" + url);
        verify(imageProcessService, never()).coverThumb(any(), anyString());
        verify(minioClient, times(1)).putObject(any());
    }

    @Test
    void oversizedCoverRejected() {
        MockMultipartFile file = image("big.jpg", "image/jpeg", 5 * 1024 * 1024 + 1);

        VideoException error = assertThrows(VideoException.class, () -> fileService.uploadVideoCover(file));

        assertEquals(202, error.getCode());
        assertTrue(error.getMessage().contains("5MB"));
    }

    @Test
    void nonImageRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1});

        VideoException error = assertThrows(VideoException.class, () -> fileService.uploadAvatar(file));

        assertEquals(202, error.getCode());
    }

    private MockMultipartFile image(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }
}
