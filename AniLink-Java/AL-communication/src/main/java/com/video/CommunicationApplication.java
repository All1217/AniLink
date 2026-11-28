package com.video;

import com.video.common.mybatisplus.MybatisPlusConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

/**
 * 聊天服务：HTTP 聊天室接口（8023）+ /chat 私信 WebSocket（Netty 8024）。
 * /video 视频房间已迁出到 al-danmaku（WS 8030）。
 *
 * <p>启动类放在 com.video 根包，保证 @SpringBootApplication 默认扫描覆盖
 * com.video.common（GlobalExceptionHandler / RedisConfiguration）与
 * com.video.communication；分页插件仍显式 @Import，避免依赖扫描顺序。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.video.api.user")
@MapperScan("com.video.communication.chat.mapper")
@Import(MybatisPlusConfiguration.class)
public class CommunicationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunicationApplication.class, args);
    }
}
