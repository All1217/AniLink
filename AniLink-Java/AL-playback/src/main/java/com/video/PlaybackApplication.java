package com.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 播放域服务：观看历史（bh_video）与播放进度合并落库。 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients(basePackages = {"com.video.api.user", "com.video.api.video"})
@MapperScan("com.video.playback.mapper")
public class PlaybackApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaybackApplication.class, args);
    }
}
