package com.video.web.admin.service.impl;

import com.video.common.constant.MinioConstant;
import com.video.common.exception.VideoException;
import com.video.common.properties.MinIOProperties;
import com.video.common.result.ResultCodeEnum;
import com.video.web.admin.mapper.AdminStorageRefMapper;
import com.video.web.admin.service.AdminStorageService;
import com.video.web.admin.vo.AdminObjectPageVo;
import com.video.web.admin.vo.AdminObjectVo;
import com.video.web.admin.vo.AdminRefVo;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
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

    private final MinioClient minioClient;
    private final MinIOProperties minIOProperties;
    private final AdminStorageRefMapper refMapper;

    public AdminStorageServiceImpl(MinioClient minioClient, MinIOProperties minIOProperties,
                                   AdminStorageRefMapper refMapper) {
        this.minioClient = minioClient;
        this.minIOProperties = minIOProperties;
        this.refMapper = refMapper;
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

    /** 汇总视频/用户/HLS 目录引用，按 type:id:field 去重。 */
    private List<AdminRefVo> findReferences(String bucket, String key) {
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
                AdminRefVo ref = refMapper.findHlsRef(Long.valueOf(parts[1]));
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
