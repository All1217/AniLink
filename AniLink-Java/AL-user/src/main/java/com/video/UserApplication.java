package com.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户域服务：登录/注册/刷新/登出/改密、用户资料与用户统计。
 * Mapper 扫描在 com.video.user.config.MapperScanConfig，避免 WebMvc 切片测试误加载 Mapper。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.video.user", "com.video.api.video", "com.video.api.social"})
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
