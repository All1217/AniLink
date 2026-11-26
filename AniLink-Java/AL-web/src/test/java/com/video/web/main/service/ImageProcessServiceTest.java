package com.video.web.main.service;

import com.video.web.main.config.ImageProperties;
import com.video.web.main.config.TranscodeProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 图片派生：用本机 ffmpeg 真实转一次 WebP。 */
class ImageProcessServiceTest {

    private static boolean ffmpegReady;

    @BeforeAll
    static void checkFfmpeg() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            process.getInputStream().readAllBytes();
            ffmpegReady = process.waitFor() == 0;
        } catch (Exception e) {
            ffmpegReady = false;
        }
    }

    @Test
    void coverWidthsAndAvatarSquare() throws Exception {
        Assumptions.assumeTrue(ffmpegReady, "本机没有 ffmpeg，跳过图片派生测试");
        ImageProcessService service = new ImageProcessService(new ImageProperties(), new TranscodeProperties());
        byte[] source = png(2000, 1200);

        byte[] large = service.coverLarge(source, ".png");
        byte[] thumb = service.coverThumb(source, ".png");
        byte[] avatar = service.avatar(source, ".png");

        assertWebp(large);
        assertWebp(thumb);
        assertWebp(avatar);
        assertEquals(960, widthOf(large));
        assertEquals(480, widthOf(thumb));
        assertEquals(192, widthOf(avatar));
        assertEquals(192, heightOf(avatar));
        assertTrue(thumb.length < large.length, "缩略图应小于主封面");
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(250, 114, 153));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static void assertWebp(byte[] data) {
        assertEquals("RIFF", new String(data, 0, 4, StandardCharsets.ISO_8859_1));
        assertEquals("WEBP", new String(data, 8, 4, StandardCharsets.ISO_8859_1));
    }

    private static int widthOf(byte[] data) {
        return dimensionOf(data, true);
    }

    private static int heightOf(byte[] data) {
        return dimensionOf(data, false);
    }

    /** 解析 WebP 头：兼容 VP8 / VP8L / VP8X 三种块 */
    private static int dimensionOf(byte[] data, boolean width) {
        String chunk = new String(data, 12, 4, StandardCharsets.ISO_8859_1);
        int payload = 20;
        switch (chunk) {
            case "VP8 " -> {
                int value = (data[payload + 6 + (width ? 0 : 2)] & 0xff)
                        | ((data[payload + 7 + (width ? 0 : 2)] & 0xff) << 8);
                return value & 0x3fff;
            }
            case "VP8L" -> {
                int bits = (data[payload + 1] & 0xff)
                        | ((data[payload + 2] & 0xff) << 8)
                        | ((data[payload + 3] & 0xff) << 16)
                        | ((data[payload + 4] & 0xff) << 24);
                int value = width ? (bits & 0x3fff) : ((bits >>> 14) & 0x3fff);
                return value + 1;
            }
            case "VP8X" -> {
                int offset = payload + (width ? 4 : 7);
                int value = (data[offset] & 0xff)
                        | ((data[offset + 1] & 0xff) << 8)
                        | ((data[offset + 2] & 0xff) << 16);
                return value + 1;
            }
            default -> throw new IllegalStateException("未知 WebP 块: " + chunk.toLowerCase(Locale.ROOT));
        }
    }
}
