package com.video.datacenter.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.video.common.result.Result;
import com.video.model.entity.Danmu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 弹幕明细 Feign 反序列化回归测试（缺陷 D2）。
 *
 * <p>al-datacenter 通过 Feign 读 al-danmaku 公开列表，响应实体复用 AL-api 的 {@link Danmu}。
 * 该实体字段带 MyBatis-Plus 的 {@code @TableField}，注解的 {@code jdbcType} 属性类型为
 * {@code org.apache.ibatis.type.JdbcType}；本模块原先只有 mybatis-plus-annotation（其 mybatis
 * 依赖为 optional 不传递），Jackson 内省字段时会抛 ClassNotFoundException，导致
 * {@code GET /main/danmu/getTagRankList} 在有弹幕时返回 code 201。</p>
 */
class DanmakuQueryClientTest {

    /** 与 al-danmaku 公开列表一致的最小响应体 */
    private static final String RESPONSE_JSON = """
            {"code":200,"message":"成功","data":[
              {"id":1,"vid":11,"uid":2,"content":"前方高能","fontsize":25,"mode":1,
               "color":"#ffffff","timePoint":1.5,"status":1}
            ]}
            """;

    @Test
    @DisplayName("Danmu 响应可被 Jackson 反序列化（@TableField 的 JdbcType 在类路径上）")
    void deserializesDanmuListWithTableFieldAnnotation() throws Exception {
        // 1.显式确认 @TableField 引用的 JdbcType 可加载：缺失时此处即抛 NoClassDefFoundError
        assertNotNull(Class.forName("org.apache.ibatis.type.JdbcType"));

        // 2.与 Feign 的 SpringDecoder 相同路径：Jackson 对 Danmu 做字段内省并反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        Result<List<Danmu>> result = objectMapper.readValue(RESPONSE_JSON,
                new TypeReference<Result<List<Danmu>>>() {
                });

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        Danmu danmu = result.getData().get(0);
        assertEquals(11L, danmu.getVid());
        assertEquals(2L, danmu.getUid());
        assertEquals("前方高能", danmu.getContent());
        assertEquals(1.5, danmu.getTimePoint().doubleValue());
    }
}
