package com.video.admin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 封面缩略图地址约定。 */
class ImageUrlsTest {

    @Test
    void thumbFromLargeWebp() {
        assertEquals("http://localhost:9000/video-cover/a_480.webp",
                ImageUrls.thumbOf("http://localhost:9000/video-cover/a_960.webp"));
    }

    @Test
    void legacyJpgHasNoThumb() {
        assertNull(ImageUrls.thumbOf("http://localhost:9000/video-cover/a.jpg"));
    }

    @Test
    void blankHasNoThumb() {
        assertNull(ImageUrls.thumbOf(null));
        assertNull(ImageUrls.thumbOf("  "));
    }
}
