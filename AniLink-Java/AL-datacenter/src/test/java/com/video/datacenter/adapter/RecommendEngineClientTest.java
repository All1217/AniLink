package com.video.datacenter.adapter;

import com.video.datacenter.config.EngineProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Flask 引擎客户端：正常返回 Long 列表，引擎不可达/报错时降级为空列表而不是抛 500。 */
class RecommendEngineClientTest {

    private EngineProperties properties;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RecommendEngineClient client;

    @BeforeEach
    void setUp() {
        properties = new EngineProperties();
        properties.setEngineUrl("http://localhost:5000");
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RecommendEngineClient(restTemplate, properties);
    }

    @Test
    @DisplayName("推荐接口：JSON 数字统一转 Long")
    void recommendReturnsLongIds() {
        server.expect(requestTo("http://localhost:5000/recommendations/7?count=10"))
                .andRespond(withSuccess("[3,1,2]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(3L, 1L, 2L), client.recommendVideoIds(7L, 10));
        server.verify();
    }

    @Test
    @DisplayName("推荐接口：count 为空时用 Flask 默认值 3")
    void recommendUsesDefaultCount() {
        server.expect(requestTo("http://localhost:5000/recommendations/7?count=3"))
                .andRespond(withSuccess("[5]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(5L), client.recommendVideoIds(7L, null));
    }

    @Test
    @DisplayName("推荐接口：引擎返回 500 时降级为空列表")
    void recommendDegradesOnServerError() {
        server.expect(requestTo("http://localhost:5000/recommendations/7?count=10"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(client.recommendVideoIds(7L, 10).isEmpty());
    }

    @Test
    @DisplayName("推荐接口：连接失败时降级为空列表")
    void recommendDegradesWhenUnreachable() {
        // 请求工厂直接抛 IOException，RestTemplate 会包成 ResourceAccessException
        ClientHttpRequestFactory broken = new ClientHttpRequestFactory() {
            @Override
            public ClientHttpRequest createRequest(java.net.URI uri, org.springframework.http.HttpMethod httpMethod)
                    throws IOException {
                throw new IOException("connection refused");
            }
        };
        RecommendEngineClient unreachable = new RecommendEngineClient(new RestTemplate(broken), properties);

        assertTrue(unreachable.recommendVideoIds(7L, 10).isEmpty());
    }

    @Test
    @DisplayName("相似用户：非数字元素被过滤，不阻断结果")
    void similarUsersFiltersNonNumbers() {
        server.expect(requestTo("http://localhost:5000/findSimilar/11/7"))
                .andRespond(withSuccess("[9,\"bad\",10]", MediaType.APPLICATION_JSON));

        assertEquals(List.of(9L, 10L), client.similarUserIds(11L, 7L));
    }

    @Test
    @DisplayName("参数为空时不发请求，直接返回空列表")
    void blankParamsReturnEmpty() {
        assertTrue(client.recommendVideoIds(null, 10).isEmpty());
        assertTrue(client.similarUserIds(null, 7L).isEmpty());
        server.verify();
    }
}
