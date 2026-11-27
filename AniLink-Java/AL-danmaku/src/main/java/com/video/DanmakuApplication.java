package com.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** 弹幕域服务：弹幕发送/公开列表（HTTP 8029）与视频房间在线人数（Netty WS 8030）。 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.video.danmaku.mapper")
public class DanmakuApplication {

    public static void main(String[] args) {
        SpringApplication.run(DanmakuApplication.class, args);
    }
}
