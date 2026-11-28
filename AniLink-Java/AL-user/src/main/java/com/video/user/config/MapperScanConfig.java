package com.video.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 用户域 Mapper 扫描：放在独立配置类，避免 WebMvc 切片测试把 Mapper 注册进来却缺少 SqlSessionFactory。
 */
@Configuration
@MapperScan("com.video.user.mapper")
public class MapperScanConfig {
}
