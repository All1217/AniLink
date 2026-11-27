package com.video.video.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片派生配置：上传时用 ffmpeg 生成 WebP 派生图，
 * 派生失败只回退原图，不影响上传本身。
 */
@Data
@Component
@ConfigurationProperties(prefix = "al.image")
public class ImageProperties {
    /** 总开关：关掉后上传保存原图，不生成派生图 */
    private boolean enabled = true;
    /** 封面 WebP 质量（0-100），78 在清晰度与体积之间比较均衡 */
    private int coverQuality = 78;
    /** 头像方图边长（像素） */
    private int avatarSize = 192;
    /** 封面原图上限：5MB */
    private long maxCoverBytes = 5L * 1024 * 1024;
    /** 头像原图上限：2MB */
    private long maxAvatarBytes = 2L * 1024 * 1024;
    /** 单张图片转换超时（秒） */
    private int timeoutSeconds = 30;
}
