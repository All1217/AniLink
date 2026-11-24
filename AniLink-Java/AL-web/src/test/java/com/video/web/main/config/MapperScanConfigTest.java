package com.video.web.main.config;

import com.video.common.mybatisplus.MybatisPlusConfiguration;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import static org.junit.jupiter.api.Assertions.*;

class MapperScanConfigTest {

    @Test
    void mapperScanIsDeclaredOnWebConfig() {
        MapperScan scan = MapperScanConfig.class.getAnnotation(MapperScan.class);
        assertNotNull(scan);
        assertArrayEquals(new String[]{"com.video.web.main.mapper"}, scan.value());
    }

    @Test
    void sharedConfigurationDoesNotHardcodeWebPackage() {
        assertNull(MybatisPlusConfiguration.class.getAnnotation(MapperScan.class));
    }
}
