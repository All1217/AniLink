package com.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 管理端服务：视频审核、用户管理、对象存储浏览/清理与封面缩略图回填。 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.video.api")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
