package com.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数据中心 BFF：数据中心统计、实时推荐与弹幕标签聚合。
 *
 * <p>本服务不拥有任何数据库表：视频日汇总走 al-video 内部契约，用户标签/摘要走 al-user，
 * 粉丝 id 集合走 al-social，弹幕明细走 al-danmaku 公开列表，Flask 算法引擎地址由配置项提供。
 * 所有跨域数据在内存组合，不出现跨域 JOIN。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.video.api.user", "com.video.api.video",
        "com.video.api.social", "com.video.datacenter.adapter"})
public class DataCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataCenterApplication.class, args);
    }
}
