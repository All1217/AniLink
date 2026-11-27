package com.video.video.service;

import com.video.video.config.ImageProperties;
import com.video.video.config.TranscodeProperties;
import com.video.video.util.FfmpegRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * 图片派生：用 ffmpeg 把上传的原图转成 WebP 多档尺寸。
 * 只做格式与尺寸转换，不落库、不碰 MinIO；失败由调用方决定是否回退原图。
 */
@Slf4j
@Service
public class ImageProcessService {

    /** 同时最多跑两个 ffmpeg，避免和 HLS 转码抢满 CPU */
    private static final int MAX_CONCURRENCY = 2;

    private final ImageProperties imageProperties;
    private final TranscodeProperties transcodeProperties;
    private final Semaphore permits = new Semaphore(MAX_CONCURRENCY);

    public ImageProcessService(ImageProperties imageProperties, TranscodeProperties transcodeProperties) {
        this.imageProperties = imageProperties;
        this.transcodeProperties = transcodeProperties;
    }

    /** 详情/海报用的大封面（960w） */
    public byte[] coverLarge(byte[] source, String ext) throws IOException {
        return resizeByWidth(source, ext, 960);
    }

    /** 列表用的小封面（480w） */
    public byte[] coverThumb(byte[] source, String ext) throws IOException {
        return resizeByWidth(source, ext, 480);
    }

    /** 头像方图：等比放大后居中裁切，保证不变形 */
    public byte[] avatar(byte[] source, String ext) throws IOException {
        int size = imageProperties.getAvatarSize();
        String filter = "scale=" + size + ":" + size + ":force_original_aspect_ratio=increase,crop=" + size + ":" + size;
        return convert(source, ext, filter);
    }

    /** 按目标宽度等比缩放；原图更窄时不放大 */
    private byte[] resizeByWidth(byte[] source, String ext, int width) throws IOException {
        String filter = "scale='min(" + width + ",iw)':-2";
        return convert(source, ext, filter);
    }

    /**
     * 写临时文件 → 调 ffmpeg → 读回 WebP 字节，无论成功失败都清理临时目录。
     */
    private byte[] convert(byte[] source, String ext, String filter) throws IOException {
        Path workDir = null;
        boolean acquired = false;
        try {
            workDir = createWorkDir();
            Path input = workDir.resolve("input." + safeExt(ext));
            Path output = workDir.resolve("output.webp");
            Files.write(input, source);

            List<String> command = List.of(
                    ffmpegPath(),
                    "-y",
                    "-i", input.getFileName().toString(),
                    "-vf", filter,
                    "-frames:v", "1",
                    "-c:v", "libwebp",
                    "-quality", String.valueOf(imageProperties.getCoverQuality()),
                    "-compression_level", "4",
                    output.getFileName().toString()
            );
            permits.acquire();
            acquired = true;
            FfmpegRunner.runSeconds(command, workDir.toFile(), imageProperties.getTimeoutSeconds());
            if (!Files.exists(output) || Files.size(output) == 0) {
                throw new IOException("ffmpeg 未生成 WebP 输出");
            }
            return Files.readAllBytes(output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("图片转换被中断", e);
        } finally {
            if (acquired) {
                permits.release();
            }
            deleteQuietly(workDir);
        }
    }

    /** 临时目录：沿用转码配置的 tmp-dir，未配置则放系统临时目录 */
    private Path createWorkDir() throws IOException {
        String base = transcodeProperties.getTmpDir();
        Path baseDir = (base == null || base.isBlank())
                ? Path.of(System.getProperty("java.io.tmpdir"), "anilink-images")
                : Path.of(base, "images");
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, "img-");
    }

    private String ffmpegPath() {
        String path = transcodeProperties.getFfmpegPath();
        return (path == null || path.isBlank()) ? "ffmpeg" : path;
    }

    /** 只保留字母数字做后缀，防止文件名注入 */
    private String safeExt(String ext) {
        if (ext == null) {
            return "img";
        }
        String cleaned = ext.replace(".", "").replaceAll("[^a-zA-Z0-9]", "");
        return cleaned.isEmpty() ? "img" : cleaned;
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.debug("清理图片临时文件失败：{}", path, e);
                }
            });
        } catch (IOException e) {
            log.debug("遍历图片临时目录失败：{}", dir, e);
        }
    }
}
