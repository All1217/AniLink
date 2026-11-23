package com.video.web.main.task;

import com.video.web.main.config.TranscodeProperties;
import com.video.web.main.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动补跑：把上次没转完的稿件重新入队。
 * 只在应用启动时执行一次，入队即返回，不阻塞启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodeRecoveryRunner implements ApplicationRunner {

    private final TranscodeService transcodeService;
    private final TranscodeProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || !properties.isRecoverOnStart()) return;
        log.info("开始检查待转码稿件（HLS）");
        transcodeService.submitPending();
    }
}
