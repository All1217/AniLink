package com.video.web.main.service.impl;

import com.video.common.constant.MinioConstant;
import com.video.common.properties.MinIOProperties;
import com.video.model.entity.Video;
import com.video.web.main.config.TranscodeProperties;
import com.video.web.main.mapper.VideoMapper;
import com.video.web.main.service.TranscodeService;
import com.video.web.main.util.FfmpegRunner;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HLS 转码：MinIO 拉源 → ffmpeg 重封装 faststart + 转 fMP4 分片 → 回传 hls/{vid}/。
 * 转码在单线程池里异步跑，失败只把原因写进 transcode_error，不影响原 MP4 播放。
 */
@Slf4j
@Service
public class TranscodeServiceImpl implements TranscodeService {

    /** 与 video.transcode_status 列注释保持一致 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_FAILED = 3;
    private static final String IMMUTABLE_CACHE = "public, max-age=31536000, immutable";

    @Autowired
    private VideoMapper videoMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinIOProperties minIOProperties;
    @Autowired
    private TranscodeProperties properties;
    @Autowired
    @Qualifier("transcodeExecutor")
    private ThreadPoolTaskExecutor executor;

    @Override
    public void submit(Long vid) {
        if (!properties.isEnabled() || vid == null) return;
        try {
            executor.execute(() -> transcode(vid));
        } catch (TaskRejectedException e) {
            log.warn("转码队列已满，稿件 {} 本次跳过（下次启动会补跑）", vid);
        }
    }

    @Override
    public void submitPending() {
        if (!properties.isEnabled()) return;
        int reset = videoMapper.resetTranscoding();
        List<Video> pending = videoMapper.listPendingTranscode();
        log.info("HLS 转码队列：待转码 {} 条，重置中断 {} 条", pending.size(), reset);
        pending.forEach(video -> submit(video.getVid()));
    }

    /** 单个稿件：失败按 maxAttempts 重试，全部失败落 STATUS_FAILED */
    private void transcode(Long vid) {
        Video video = videoMapper.getRawById(vid);
        if (video == null || !StringUtils.hasText(video.getVideoUrl())) {
            log.warn("稿件 {} 不存在或没有视频地址，跳过转码", vid);
            return;
        }
        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            try {
                videoMapper.updateTranscodeStatus(vid, STATUS_RUNNING, null);
                String hlsUrl = transcodeOnce(video);
                videoMapper.updateTranscodeDone(vid, hlsUrl);
                log.info("稿件 {} 转码完成：{}", vid, hlsUrl);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                videoMapper.updateTranscodeStatus(vid, STATUS_PENDING, "线程被中断");
                return;
            } catch (Exception e) {
                log.warn("稿件 {} 第 {}/{} 次转码失败：{}", vid, attempt, properties.getMaxAttempts(), e.getMessage());
                if (attempt == properties.getMaxAttempts()) {
                    videoMapper.updateTranscodeStatus(vid, STATUS_FAILED, truncate(e.getMessage(), 480));
                } else {
                    sleep(2000L);
                }
            }
        }
    }

    /** 一次完整转码：faststart 重封装（回写原对象）+ fMP4 分片 HLS，返回 m3u8 地址 */
    private String transcodeOnce(Video video) throws Exception {
        File workDir = new File(tmpRoot(), "video-" + video.getVid() + "-" + System.currentTimeMillis());
        if (!workDir.mkdirs()) throw new IOException("无法创建转码临时目录：" + workDir);
        try {
            // ① 重封装：moov 前置，保证没有 HLS 时原 MP4 也能快速起播（对象名不变，URL 不变）
            File faststart = new File(workDir, "faststart.mp4");
            List<String> remux = new ArrayList<>(ffmpegBase());
            remux.addAll(List.of("-i", video.getVideoUrl(), "-c", "copy",
                    "-movflags", "+faststart", "-f", "mp4", faststart.getAbsolutePath()));
            FfmpegRunner.run(remux, workDir, properties.getTimeoutMinutes());
            uploadFaststart(video, faststart);

            // ② 转 fMP4 分片 HLS：进程工作目录 = 产物目录，init.mp4 才会落在播放列表旁边
            File hlsDir = new File(workDir, "hls");
            if (!hlsDir.mkdirs()) throw new IOException("无法创建 HLS 产物目录：" + hlsDir);
            int capKbps = bitrateCap(video.getVideoUrl());
            log.info("稿件 {} 转码：视频码率上限 {} kbps", video.getVid(), capKbps);
            FfmpegRunner.run(hlsCommand(faststart, capKbps), hlsDir, properties.getTimeoutMinutes());

            // ③ 分片与 m3u8 回传 MinIO
            return uploadHls(video.getVid(), hlsDir);
        } finally {
            FileSystemUtils.deleteRecursively(workDir);
        }
    }

    /** ffmpeg 公共前缀参数 */
    private List<String> ffmpegBase() {
        return new ArrayList<>(List.of(properties.getFfmpegPath(), "-y", "-hide_banner", "-loglevel", "error"));
    }

    /** HLS 转码参数：6s 分片、强制按分片边界打关键帧、音频统一 AAC，视频码率按上限封顶 */
    private List<String> hlsCommand(File source, int maxKbps) {
        int segment = Math.max(2, properties.getSegmentSeconds());
        return List.of(
                properties.getFfmpegPath(), "-y", "-hide_banner", "-loglevel", "error",
                "-i", source.getAbsolutePath(),
                "-c:v", "libx264",
                "-preset", properties.getPreset(),
                "-crf", String.valueOf(properties.getCrf()),
                // CRF 搭配 maxrate/bufsize：画质按 CRF 走，但整段码率不超过上限
                "-maxrate", maxKbps + "k",
                "-bufsize", maxKbps * 2 + "k",
                "-pix_fmt", "yuv420p",
                "-force_key_frames", "expr:gte(t,n_forced*" + segment + ")",
                "-sc_threshold", "0",
                "-c:a", "aac", "-b:a", "128k", "-ac", "2",
                "-f", "hls",
                "-hls_time", String.valueOf(segment),
                "-hls_playlist_type", "vod",
                "-hls_segment_type", "fmp4",
                "-hls_flags", "independent_segments",
                "-hls_segment_filename", "seg_%04d.m4s",
                "index.m3u8"
        );
    }

    /** 码率上限：取源码率的 1.15 倍，避免重编码把已压过的视频码率抬上去，并受配置上限约束 */
    private int bitrateCap(String url) {
        long sourceBitrate = probeSourceBitrate(url);
        int cap = sourceBitrate > 0
                ? (int) Math.round(sourceBitrate / 1000.0 * 1.15)
                : properties.getMaxBitrateKbps();
        return Math.max(300, Math.min(cap, properties.getMaxBitrateKbps()));
    }

    /** 用 ffprobe 读源码率（bit/s）；读不到返回 0，由调用方回退到配置上限 */
    private long probeSourceBitrate(String url) {
        List<String> command = List.of(ffprobePath(), "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=bit_rate:format=bit_rate",
                "-of", "default=noprint_wrappers=1:nokey=1", url);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return 0;
            }
            for (String line : output.split("\\R")) {
                String text = line.trim();
                if (text.matches("\\d+")) return Long.parseLong(text);
            }
        } catch (Exception e) {
            log.debug("ffprobe 读取源码率失败：{}", e.getMessage());
        }
        return 0;
    }

    /** ffprobe 路径：优先配置，否则取 ffmpeg 同目录（ffmpeg.exe → ffprobe.exe） */
    private String ffprobePath() {
        String configured = properties.getFfprobePath();
        if (StringUtils.hasText(configured)) return configured;
        Path ffmpeg = Path.of(properties.getFfmpegPath());
        Path parent = ffmpeg.getParent();
        if (parent == null) return "ffprobe";
        String name = ffmpeg.getFileName().toString().replace("ffmpeg", "ffprobe");
        Path probe = parent.resolve(name);
        return Files.exists(probe) ? probe.toString() : "ffprobe";
    }

    /** 回写 faststart 后的 MP4，覆盖原对象（可用性优先，失败不阻断 HLS） */
    private void uploadFaststart(Video video, File file) throws Exception {
        String objectName = objectNameOf(video.getVideoUrl());
        if (objectName == null) {
            log.warn("稿件 {} 的视频地址无法解析对象名，跳过 faststart 回写", video.getVid());
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(MinioConstant.VIDEO_BUCKET)
                    .object(objectName)
                    .stream(in, file.length(), -1)
                    .contentType("video/mp4")
                    .headers(Map.of("Cache-Control", IMMUTABLE_CACHE))
                    .build());
        }
    }

    /** 上传 hls/{vid}/ 下的全部分片与播放列表，返回可直接播放的 m3u8 地址 */
    private String uploadHls(Long vid, File hlsDir) throws Exception {
        File[] files = hlsDir.listFiles();
        if (files == null || files.length == 0) throw new IllegalStateException("HLS 产物为空");
        for (File file : files) {
            boolean playlist = file.getName().endsWith(".m3u8");
            try (InputStream in = new FileInputStream(file)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(MinioConstant.VIDEO_BUCKET)
                        .object(MinioConstant.HLS_PREFIX + vid + "/" + file.getName())
                        .stream(in, file.length(), -1)
                        .contentType(playlist ? "application/vnd.apple.mpegurl" : "video/mp4")
                        .headers(Map.of("Cache-Control", playlist ? "public, max-age=60" : IMMUTABLE_CACHE))
                        .build());
            }
        }
        return endpoint() + "/" + MinioConstant.VIDEO_BUCKET + "/"
                + MinioConstant.HLS_PREFIX + vid + "/index.m3u8";
    }

    /** 转码临时目录：默认系统临时目录下的 anilink-hls */
    private File tmpRoot() {
        String dir = properties.getTmpDir();
        File root = (dir == null || dir.isBlank())
                ? new File(System.getProperty("java.io.tmpdir"), "anilink-hls")
                : new File(dir);
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException("无法创建转码临时目录：" + root);
        }
        return root;
    }

    /** MinIO endpoint 去掉结尾斜杠，拼 URL 时不出现双斜杠 */
    private String endpoint() {
        String endpoint = minIOProperties.getEndpoint();
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    /** 从 .../video/<uuid>.mp4 里取对象名 */
    private static String objectNameOf(String url) {
        if (!StringUtils.hasText(url)) return null;
        int index = url.lastIndexOf('/');
        return index > 0 && index < url.length() - 1 ? url.substring(index + 1) : null;
    }

    private static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
