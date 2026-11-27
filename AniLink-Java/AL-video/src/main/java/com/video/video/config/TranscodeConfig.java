package com.video.video.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 转码线程池：ffmpeg 很吃 CPU，固定单线程串行执行，
 * 既避免并发转码把机器打满，也保证队列里按投稿顺序处理。
 */
@Configuration
public class TranscodeConfig {

    @Bean("transcodeExecutor")
    public ThreadPoolTaskExecutor transcodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("video-transcode-");
        // 停机时不等转码收尾，残留任务下次启动由 transcode_status 补跑
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
