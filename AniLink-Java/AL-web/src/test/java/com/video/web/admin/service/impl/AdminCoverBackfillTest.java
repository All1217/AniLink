package com.video.web.admin.service.impl;

import com.video.common.properties.MinIOProperties;
import com.video.web.admin.mapper.AdminCoverBackfillMapper;
import com.video.web.admin.mapper.AdminStorageRefMapper;
import com.video.web.admin.vo.AdminBackfillResultVo;
import com.video.web.admin.vo.AdminCoverBackfillVo;
import com.video.web.main.service.ImageProcessService;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 存量封面缩略图回填：成功写回、外链跳过、幂等。 */
@ExtendWith(MockitoExtension.class)
class AdminCoverBackfillTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinIOProperties minIOProperties;
    @Mock
    private AdminStorageRefMapper refMapper;
    @Mock
    private AdminCoverBackfillMapper backfillMapper;
    @Mock
    private ImageProcessService imageProcessService;
    @InjectMocks
    private AdminStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(minIOProperties.getEndpoint()).thenReturn("http://localhost:9000");
    }

    @Test
    void writesThumbForOwnCover() throws Exception {
        AdminCoverBackfillVo row = row(1L, "http://localhost:9000/video-cover/a.jpg");
        when(backfillMapper.listMissingThumbs(0L, 50)).thenReturn(List.of(row));
        when(imageProcessService.coverThumb(any(), anyString())).thenReturn(new byte[]{1});
        when(backfillMapper.updateThumb(1L, "http://localhost:9000/video-cover/a_480.webp")).thenReturn(1);

        AdminStorageServiceImpl spyService = spy(service);
        doReturn(new byte[]{9}).when(spyService).readObject("video-cover", "a.jpg");

        AdminBackfillResultVo result = spyService.backfillCoverThumbs(9L);

        assertEquals(1, result.getScanned());
        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getSkipped());
        assertEquals(0, result.getFailed());
        verify(minioClient).putObject(any());
    }

    @Test
    void skipsExternalCover() throws Exception {
        when(backfillMapper.listMissingThumbs(0L, 50))
                .thenReturn(List.of(row(2L, "https://cdn.example.com/a.jpg")));

        AdminBackfillResultVo result = service.backfillCoverThumbs(9L);

        assertEquals(1, result.getScanned());
        assertEquals(1, result.getSkipped());
        assertEquals(0, result.getUpdated());
        verify(minioClient, never()).putObject(any());
    }

    @Test
    void idempotentWhenNoMissingCovers() {
        when(backfillMapper.listMissingThumbs(0L, 50)).thenReturn(List.of());

        AdminBackfillResultVo result = service.backfillCoverThumbs(9L);

        assertEquals(0, result.getScanned());
        assertEquals(0, result.getUpdated());
        verify(backfillMapper, never()).updateThumb(any(), anyString());
    }

    private AdminCoverBackfillVo row(Long vid, String coverUrl) {
        AdminCoverBackfillVo vo = new AdminCoverBackfillVo();
        vo.setVid(vid);
        vo.setCoverUrl(coverUrl);
        return vo;
    }
}
