package com.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 社交域服务：关注/粉丝（follow）与收藏夹（favorite、favorite_video）。
 *
 * <p>只读写本域属主表；用户信息走 al-user 内部批量接口，视频信息走 al-video 内部批量接口；
 * 收藏/取消收藏提交后投递 {@code favorite.changed} 事件，由 al-video 幂等更新视频收藏计数。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.video.api.user", "com.video.api.video"})
@MapperScan("com.video.social.mapper")
public class SocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
