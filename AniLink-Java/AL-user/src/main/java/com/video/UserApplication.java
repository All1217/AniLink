package com.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** 用户域服务：登录/注册/刷新/登出/改密、用户资料与用户统计。 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.video.user")
@MapperScan("com.video.user.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
