package com.video.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GatewayRoutesConfigTest {

    @Test
    void loadsPhase3Routes() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("routes", new ClassPathResource("routes.yml"))
                .forEach(environment.getPropertySources()::addLast);

        List<RouteDefinition> routes = Binder.get(environment)
                .bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class))
                .orElse(List.of());
        Map<String, RouteDefinition> byId = routes.stream()
                .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));

        assertEquals(11, routes.size());
        assertEquals(11, byId.size(), "路由 id 不能重复");

        assertRoute(byId, "user-service", "lb://userservice", -1);
        assertRoute(byId, "playback-service", "lb://playbackservice", -2);
        assertRoute(byId, "video-recommend-service", "lb://webservice", -2);
        assertRoute(byId, "video-service", "lb://videoservice", -1);
        assertRoute(byId, "danmaku-recommend-service", "lb://webservice", -2);
        assertRoute(byId, "comment-service", "lb://commentservice", -1);
        assertRoute(byId, "danmaku-service", "lb://danmakuservice", -1);
        assertRoute(byId, "social-service", "lb://socialservice", -1);
        assertRoute(byId, "chat-service", "lb://communicationservice", -1);
        assertRoute(byId, "remark-service", "lb://remarkservice", 0);
        assertRoute(byId, "web-service", "lb://webservice", 0);
    }

    private void assertRoute(Map<String, RouteDefinition> byId, String id, String uri, int order) {
        RouteDefinition route = byId.get(id);
        assertNotNull(route, "缺少路由 " + id);
        assertEquals(uri, route.getUri().toString());
        assertEquals(order, route.getOrder(), id + " 的 order 不正确");
    }
}
