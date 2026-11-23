package com.video.web.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HLS 转码配置：转码在 AL-web 进程内异步执行（单线程队列），
 * 转码完成前播放器继续使用原 MP4，失败不影响播放。
 */
@Data
@Component
@ConfigurationProperties(prefix = "al.transcode")
public class TranscodeProperties {
    /** 总开关：关掉后投稿不再触发转码，也不在启动时补跑 */
    private boolean enabled = true;
    /** ffmpeg 可执行文件；换机器时用环境变量 AL_FFMPEG_PATH 覆盖 */
    private String ffmpegPath = "ffmpeg";
    /** 临时目录，为空则用系统临时目录下的 anilink-hls */
    private String tmpDir = "";
    /** x264 预设：越慢画质越好，veryfast 用于本地开发 */
    private String preset = "veryfast";
    /** x264 CRF，18~23 之间，数值越小越清晰 */
    private int crf = 21;
    /** HLS 分片时长（秒） */
    private int segmentSeconds = 6;
    /** 单个视频最多尝试次数（含首次） */
    private int maxAttempts = 2;
    /** 启动时把 transcode_status=0 的存量稿件补跑一遍 */
    private boolean recoverOnStart = true;
    /** 单次 ffmpeg 的超时时间（分钟），长视频要留足 */
    private int timeoutMinutes = 60;
}
