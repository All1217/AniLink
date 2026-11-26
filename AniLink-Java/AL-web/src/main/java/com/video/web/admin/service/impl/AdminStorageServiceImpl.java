package com.video.web.admin.service.impl;

import com.video.common.constant.MinioConstant;
import com.video.common.exception.VideoException;
import com.video.common.properties.MinIOProperties;
import com.video.common.result.ResultCodeEnum;
import com.video.web.admin.mapper.AdminCoverBackfillMapper;
import com.video.web.admin.mapper.AdminStorageRefMapper;
import com.video.web.admin.service.AdminStorageService;
import com.video.web.admin.vo.AdminBackfillResultVo;
import com.video.web.admin.vo.AdminCleanupResultVo;
import com.video.web.admin.vo.AdminCoverBackfillVo;
import com.video.web.admin.vo.AdminObjectPageVo;
import com.video.web.admin.vo.AdminObjectVo;
import com.video.web.admin.vo.AdminRefVo;
import com.video.web.main.service.ImageProcessService;
import com.video.web.main.util.ImageUrls;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 对象存储管理：浏览、预签名预览、带引用检查的真实删除。 */
@Slf4j
@Service
public class AdminStorageServiceImpl implements AdminStorageService {

    private static final List<String> ALLOWED_BUCKETS = List.of(
            MinioConstant.AVATAR_BUCKET,
            MinioConstant.VIDEO_BUCKET,
            MinioConstant.VIDEO_COVER_BUCKET
    );
    private static final int CLEANUP_PAGE_SIZE = 200;
    private static final int BACKFILL_PAGE_SIZE = 50;
    private static final String WEBP_CONTENT_TYPE = "image/webp";
    private static final String IMMUTABLE_CACHE = "public, max-age=31536000, immutable";

    private final MinioClient minioClient;
    private final MinIOProperties minIOProperties;
    private final AdminStorageRefMapper refMapper;
    private final AdminCoverBackfillMapper backfillMapper;
    private final ImageProcessService imageProcessService;

    public AdminStorageServiceImpl(MinioClient minioClient, MinIOProperties minIOProperties,
                                   AdminStorageRefMapper refMapper, AdminCoverBackfillMapper backfillMapper,
                                   ImageProcessService imageProcessService) {
        this.minioClient = minioClient;
        this.minIOProperties = minIOProperties;
        this.refMapper = refMapper;
        this.backfillMapper = backfillMapper;
        this.imageProcessService = imageProcessService;
    }

    @Override
    public List<String> listBuckets() {
        return ALLOWED_BUCKETS;
    }

    @Override
    public AdminObjectPageVo listObjects(String bucket, String prefix, String cursor, int size) {
        validateBucket(bucket);
        int limit = Math.min(200, Math.max(1, size));
        String normalizedPrefix = prefix == null ? "" : prefix;
        List<AdminObjectVo> items = new ArrayList<>();
        try {
            ListObjectsArgs.Builder builder = ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(normalizedPrefix)
                    .recursive(true)
                    .maxKeys(limit);
            if (cursor != null && !cursor.isBlank()) {
                builder.startAfter(cursor);
            }
            Iterable<Result<Item>> results = minioClient.listObjects(builder.build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }
                AdminObjectVo vo = new AdminObjectVo();
                vo.setBucket(bucket);
                vo.setKey(item.objectName());
                vo.setSize(item.size());
                vo.setLastModified(Date.from(item.lastModified().toInstant()));
                items.add(vo);
            }
        } catch (Exception e) {
            log.error("读取 MinIO 对象失败：bucket={}, prefix={}", bucket, normalizedPrefix, e);
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "读取对象列表失败");
        }

        AdminObjectPageVo page = new AdminObjectPageVo();
        page.setItems(items);
        page.setHasMore(items.size() >= limit);
        page.setNextCursor(items.isEmpty() ? null : items.get(items.size() - 1).getKey());
        return page;
    }

    @Override
    public String presign(String bucket, String key) {
        validateBucket(bucket);
        validateKey(key);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(5, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成 MinIO 预览地址失败：bucket={}, key={}", bucket, key, e);
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "生成预览地址失败");
        }
    }

    @Override
    public List<AdminRefVo> delete(String bucket, String key, Long operatorUid) {
        validateBucket(bucket);
        validateKey(key);
        List<AdminRefVo> refs = findReferences(bucket, key);
        if (!refs.isEmpty()) {
            log.info("admin action operator={} action=storage.delete.blocked bucket={} key={} refs={}",
                    operatorUid, bucket, key, refs.size());
            return refs;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败：bucket={}, key={}", bucket, key, e);
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "删除对象失败");
        }
        log.info("admin action operator={} action=storage.delete bucket={} key={}",
                operatorUid, bucket, key);
        return List.of();
    }

    @Override
    public AdminCleanupResultVo cleanup(String bucket, String prefix, Long operatorUid) {
        validateBucket(bucket);
        String normalizedPrefix = prefix == null ? "" : prefix;
        AdminCleanupResultVo result = new AdminCleanupResultVo();
        Map<Long, AdminRefVo> hlsCache = new HashMap<>();
        String cursor = null;
        while (true) {
            AdminObjectPageVo page = listObjects(bucket, normalizedPrefix, cursor, CLEANUP_PAGE_SIZE);
            if (page.getItems().isEmpty()) {
                break;
            }
            for (AdminObjectVo object : page.getItems()) {
                result.setScanned(result.getScanned() + 1);
                List<AdminRefVo> refs = findReferences(bucket, object.getKey(), hlsCache);
                if (!refs.isEmpty()) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket).object(object.getKey()).build());
                    result.setDeleted(result.getDeleted() + 1);
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailedKeys().add(object.getKey());
                    log.warn("清理 MinIO 对象失败：bucket={}, key={}", bucket, object.getKey(), e);
                }
            }
            if (!page.isHasMore() || page.getNextCursor() == null) {
                break;
            }
            cursor = page.getNextCursor();
        }
        log.info("admin action operator={} action=storage.cleanup bucket={} prefix={} "
                        + "scanned={} deleted={} skipped={} failed={}",
                operatorUid, bucket, normalizedPrefix, result.getScanned(),
                result.getDeleted(), result.getSkipped(), result.getFailed());
        return result;
    }

    @Override
    public AdminBackfillResultVo backfillCoverThumbs(Long operatorUid) {
        AdminBackfillResultVo result = new AdminBackfillResultVo();
        String endpoint = endpoint();
        if (endpoint == null) {
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "MinIO 地址未配置");
        }
        // 按 vid 游标分批，只处理 cover_thumb_url 为空的稿件，重复触发不会重复生成
        long afterVid = 0L;
        while (true) {
            List<AdminCoverBackfillVo> batch = backfillMapper.listMissingThumbs(afterVid, BACKFILL_PAGE_SIZE);
            if (batch.isEmpty()) {
                break;
            }
            for (AdminCoverBackfillVo row : batch) {
                afterVid = row.getVid();
                result.setScanned(result.getScanned() + 1);
                String key = videoCoverKey(row.getCoverUrl(), endpoint);
                if (key == null) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }
                String thumbKey = thumbKeyOf(key);
                String thumbUrl = endpoint + "/" + MinioConstant.VIDEO_COVER_BUCKET + "/" + thumbKey;
                try {
                    byte[] source = readObject(MinioConstant.VIDEO_COVER_BUCKET, key);
                    byte[] thumb = imageProcessService.coverThumb(source, extOf(key));
                    putBytes(MinioConstant.VIDEO_COVER_BUCKET, thumbKey, thumb);
                    if (backfillMapper.updateThumb(row.getVid(), thumbUrl) == 1) {
                        result.setUpdated(result.getUpdated() + 1);
                    } else {
                        result.setSkipped(result.getSkipped() + 1);
                    }
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    log.warn("回填封面缩略图失败：vid={}, cover={}", row.getVid(), row.getCoverUrl(), e);
                }
            }
            if (batch.size() < BACKFILL_PAGE_SIZE) {
                break;
            }
        }
        log.info("admin action operator={} action=image.backfill scanned={} updated={} skipped={} failed={}",
                operatorUid, result.getScanned(), result.getUpdated(), result.getSkipped(), result.getFailed());
        return result;
    }

    /** 读对象内容；独立成 protected 便于单测覆盖，不依赖真实 MinIO */
    protected byte[] readObject(String bucket, String key) throws Exception {
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return in.readAllBytes();
        }
    }

    /** 派生图字节直接入桶，沿用长期不可变缓存 */
    private void putBytes(String bucket, String key, byte[] data) throws Exception {
        try (InputStream in = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, data.length, -1)
                    .contentType(WEBP_CONTENT_TYPE)
                    .headers(Map.of("Cache-Control", IMMUTABLE_CACHE))
                    .build());
        }
    }

    /** 只处理本 MinIO 的 video-cover 对象，外链封面计入跳过 */
    private String videoCoverKey(String url, String endpoint) {
        String prefix = endpoint + "/" + MinioConstant.VIDEO_COVER_BUCKET + "/";
        if (url == null || !url.startsWith(prefix)) {
            return null;
        }
        String key = url.substring(prefix.length());
        return key.isBlank() ? null : key;
    }

    /** 旧封面名可能是 uuid.jpg，统一派生成 uuid_480.webp */
    private String thumbKeyOf(String key) {
        if (key.endsWith(ImageUrls.LARGE_SUFFIX)) {
            return key.substring(0, key.length() - ImageUrls.LARGE_SUFFIX.length()) + ImageUrls.THUMB_SUFFIX;
        }
        int dot = key.lastIndexOf('.');
        String base = dot < 0 ? key : key.substring(0, dot);
        return base + ImageUrls.THUMB_SUFFIX;
    }

    /** 对象后缀，用于告诉 ffmpeg 输入格式 */
    private String extOf(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(dot);
    }

    private String endpoint() {
        String endpoint = minIOProperties.getEndpoint();
        return (endpoint == null || endpoint.isBlank()) ? null : endpoint.replaceAll("/+$", "");
    }

    /** 汇总视频/用户/HLS 目录引用，按 type:id:field 去重。 */
    private List<AdminRefVo> findReferences(String bucket, String key) {
        return findReferences(bucket, key, new HashMap<>());
    }

    /** 清理时复用 HLS vid 缓存，避免同一目录下每个分片都查一次稿件。 */
    private List<AdminRefVo> findReferences(String bucket, String key, Map<Long, AdminRefVo> hlsCache) {
        String endpoint = minIOProperties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new VideoException(ResultCodeEnum.APP_SERVER_ERROR.getCode(), "MinIO 地址未配置");
        }
        String url = endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + key;
        Map<String, AdminRefVo> refs = new LinkedHashMap<>();
        for (AdminRefVo ref : refMapper.findVideoRefs(url)) {
            refs.putIfAbsent(refKey(ref), ref);
        }
        for (AdminRefVo ref : refMapper.findUserRefs(url)) {
            refs.putIfAbsent(refKey(ref), ref);
        }
        if (MinioConstant.VIDEO_BUCKET.equals(bucket) && key.startsWith(MinioConstant.HLS_PREFIX)) {
            String[] parts = key.split("/");
            if (parts.length > 1 && parts[1].matches("\\d+")) {
                Long vid = Long.valueOf(parts[1]);
                AdminRefVo ref;
                if (hlsCache.containsKey(vid)) {
                    ref = hlsCache.get(vid);
                } else {
                    ref = refMapper.findHlsRef(vid);
                    hlsCache.put(vid, ref);
                }
                if (ref != null) {
                    refs.putIfAbsent(refKey(ref), ref);
                }
            }
        }
        return new ArrayList<>(refs.values());
    }

    private String refKey(AdminRefVo ref) {
        return ref.getType() + ":" + ref.getId() + ":" + ref.getField();
    }

    private void validateBucket(String bucket) {
        if (bucket == null || !ALLOWED_BUCKETS.contains(bucket)) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "不允许访问该桶");
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/")) {
            throw new VideoException(ResultCodeEnum.PARAM_ERROR.getCode(), "对象 key 不合法");
        }
    }
}
