package com.video.web.main.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** al.image 配置默认值。 */
class ImagePropertiesTest {

    @Test
    void defaultsBound() {
        ImageProperties props = Binder.get(new StandardEnvironment())
                .bind("al.image", Bindable.of(ImageProperties.class))
                .orElseGet(ImageProperties::new);

        assertTrue(props.isEnabled());
        assertEquals(78, props.getCoverQuality());
        assertEquals(192, props.getAvatarSize());
        assertEquals(5L * 1024 * 1024, props.getMaxCoverBytes());
        assertEquals(2L * 1024 * 1024, props.getMaxAvatarBytes());
        assertEquals(30, props.getTimeoutSeconds());
    }
}
