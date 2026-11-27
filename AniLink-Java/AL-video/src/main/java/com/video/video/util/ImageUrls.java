package com.video.video.util;

/**
 * 封面派生图地址约定。
 * 上传成功后封面固定保存为 {uuid}_480.webp 与 {uuid}_960.webp，
 * 因此 480 缩略图地址可以由 960 主图地址推导，无需额外查库。
 */
public final class ImageUrls {

    /** 详情/海报用的主封面后缀 */
    public static final String LARGE_SUFFIX = "_960.webp";
    /** 列表用的缩略图后缀 */
    public static final String THUMB_SUFFIX = "_480.webp";

    private ImageUrls() {
    }

    /**
     * 由 960w 主封面推导 480w 缩略图地址。
     *
     * @param coverUrl 主封面地址
     * @return 缩略图地址；地址为空或不符合同约定（旧数据、派生失败回退）时返回 null
     */
    public static String thumbOf(String coverUrl) {
        if (coverUrl == null) {
            return null;
        }
        String url = coverUrl.trim();
        if (url.isEmpty() || !url.endsWith(LARGE_SUFFIX)) {
            return null;
        }
        return url.substring(0, url.length() - LARGE_SUFFIX.length()) + THUMB_SUFFIX;
    }
}
