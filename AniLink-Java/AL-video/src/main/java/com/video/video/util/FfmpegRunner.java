package com.video.video.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ffmpeg 进程调用封装。
 * 约定：输出文件用相对路径，并把工作目录设为产物目录——
 * fMP4 的 init.mp4 由 ffmpeg 写到**进程工作目录**，否则播放列表里的
 * EXT-X-MAP 会指向一个不存在的位置。
 */
@Slf4j
public final class FfmpegRunner {

    private FfmpegRunner() {
    }

    /**
     * 同步执行 ffmpeg，非 0 退出码抛异常（带日志尾部，便于写进 transcode_error）。
     */
    public static void run(List<String> command, File workDir, int timeoutMinutes) throws IOException, InterruptedException {
        runFor(command, workDir, TimeUnit.MINUTES.toMillis(timeoutMinutes));
    }

    /** 秒级超时版本，用于图片转换这类短任务。 */
    public static void runSeconds(List<String> command, File workDir, int timeoutSeconds) throws IOException, InterruptedException {
        runFor(command, workDir, TimeUnit.SECONDS.toMillis(timeoutSeconds));
    }

    private static void runFor(List<String> command, File workDir, long timeoutMillis) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workDir)
                .redirectErrorStream(true);
        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        // 必须持续消费输出，否则管道写满会把 ffmpeg 卡死
        Thread reader = new Thread(() -> drain(process, output), "ffmpeg-output");
        reader.setDaemon(true);
        reader.start();
        if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg 执行超时（" + timeoutMillis + " 毫秒）");
        }
        reader.join(1000);
        int code = process.exitValue();
        if (code != 0) {
            throw new IllegalStateException("ffmpeg 退出码 " + code + "：" + tail(output));
        }
    }

    private static void drain(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    if (output.length() < 8000) output.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            log.debug("读取 ffmpeg 输出失败", e);
        }
    }

    private static String tail(StringBuilder output) {
        synchronized (output) {
            String text = output.toString().trim();
            return text.length() <= 1200 ? text : text.substring(text.length() - 1200);
        }
    }
}
