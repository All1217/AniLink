package com.video.common.constant;

public class MinioConstant {
    public static final String AVATAR_BUCKET = "avatar";
    public static final String VIDEO_BUCKET = "video";
    public static final String VIDEO_COVER_BUCKET = "video-cover";
    // HLS 分片在 video 桶内的对象前缀：hls/{vid}/index.m3u8
    public static final String HLS_PREFIX = "hls/";
    // 默认头像
    public static final String DEFAULT_AVATAR = "http://192.168.150.102:9000/avatar/660f3841-e5e0-4be8-b9e6-6d958a39653b.jpg";
}
