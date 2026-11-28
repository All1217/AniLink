package com.video.communication.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.video.common.mybatisplus.MybatisPlusConfiguration;
import com.video.CommunicationApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页配置回归测试（缺陷 D5）。
 *
 * <p>AL-communication 的启动类在 {@code com.video.communication} 包下，组件扫描到不了
 * {@code com.video.common.mybatisplus}，而 AL-common-mybatis 又没有自动装配声明，
 * 于是 MyBatis-Plus 分页插件从未注册：IPage 的 total 恒为 0，但 records 仍返回数据。
 * 这里校验启动类显式导入了分页配置，且配置确实提供分页内部拦截器。</p>
 */
class PaginationConfigTest {

    @Test
    @DisplayName("启动类导入分页配置，且分页内部拦截器已注册")
    void communicationApplicationRegistersPaginationInterceptor() {
        // 1.启动类必须显式 @Import AL-common-mybatis 的分页配置（包扫描覆盖不到）
        Import imported = CommunicationApplication.class.getAnnotation(Import.class);
        assertNotNull(imported, "CommunicationApplication 必须 @Import(MybatisPlusConfiguration.class)");
        assertTrue(Arrays.asList(imported.value()).contains(MybatisPlusConfiguration.class),
                "CommunicationApplication 未导入 MybatisPlusConfiguration");

        // 2.配置必须产出带 PaginationInnerInterceptor 的 MybatisPlusInterceptor，
        //   否则 IPage.total 不会被回填（getChatVoPage 有 records 但 total=0）
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(MybatisPlusConfiguration.class)) {
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            List<?> innerInterceptors = interceptor.getInterceptors();
            assertTrue(innerInterceptors.stream().anyMatch(PaginationInnerInterceptor.class::isInstance),
                    "分页内部拦截器缺失：IPage.total 会恒为 0");
        }
    }
}
