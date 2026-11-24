package com.video.web.main.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** AL-web 的 Mapper 扫描；放在独立配置类，避免 Web 切片测试加载 Mapper。 */
@Configuration
@MapperScan("com.video.web.main.mapper")
public class MapperScanConfig {
}
