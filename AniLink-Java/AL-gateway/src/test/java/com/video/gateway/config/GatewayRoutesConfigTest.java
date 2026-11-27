package com.video.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatewayRoutesConfigTest {

    @Test
    void loadsRemarkAndWebRoutes() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("routes", new ClassPathResource("routes.yml"))
                .forEach(environment.getPropertySources()::addLast);

        List<RouteDefinition> routes = Binder.get(environment)
                .bind("spring.cloud.gateway.routes", Bindable.listOf(RouteDefinition.class))
                .orElse(List.of());

        assertEquals(3, routes.size());
        assertEquals("user-service", routes.get(0).getId());
        assertEquals("lb://userservice", routes.get(0).getUri().toString());
        assertEquals(-1, routes.get(0).getOrder());
        assertEquals("remark-service", routes.get(1).getId());
        assertEquals("lb://remarkservice", routes.get(1).getUri().toString());
        assertEquals("web-service", routes.get(2).getId());
        assertEquals("lb://webservice", routes.get(2).getUri().toString());
    }
}
