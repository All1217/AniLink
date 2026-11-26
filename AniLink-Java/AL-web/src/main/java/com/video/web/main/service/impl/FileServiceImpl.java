package com.video.web.main.service.impl;

import com.video.common.properties.MinIOProperties;
import com.video.common.result.Result;
import com.video.common.exception.VideoException;
import com.video.web.main.config.ImageProperties;
import com.video.web.main.service.FileService;
import com.video.web.main.service.ImageProcessService;
import com.video.web.main.util.ImageUrls;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.video.common.constant.MinioConstant.*;
import static com.video.common.result.ResultCodeEnum.APP_SERVER_ERROR;
import static com.video.common.result.ResultCodeEnum.PARAM_ERROR;

/**
 * 文件上传：视频原样入桶，封面与头像先生成 WebP 派生图再入桶。
 * 派生失败时整体回退到原图，保证投稿/换头像不被图片处理拖垮。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    /** 按后缀写正确的 MIME：MinIO 默认给 application/octet-stream，浏览器会退化成嗅探 */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".mp4", "video/mp4",
            ".webm", "video/webm",
            ".mov", "video/quicktime",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".webp", "image/webp",
            ".gif", "image/gif",
            ".svg", "image/svg+xml"
    );
    /** 允许上传的图片后缀 */
    private static final Set<String> IMAGE_EXTS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp");
    /** 对象名含 UUID，内容不会变，可按长期不可变缓存 */
    private static final String IMMUTABLE_CACHE = "public, max-age=31536000, immutable";
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final MinioClient minioClient;
    private final MinIOProperties minIOProperties;
    private final ImageProperties imageProperties;
    private final ImageProcessService imageProcessService;

    @Override
    public Result uploadAvatar(MultipartFile file) {
        validateImage(file, imageProperties.getMaxAvatarBytes(), "头像");
        return uploadDerived(file, AVATAR_BUCKET, true);
    }

    @Override
    public Result uploadVideoCover(MultipartFile file) {
        validateImage(file, imageProperties.getMaxCoverBytes(), "封面");
        return uploadDerived(file, VIDEO_COVER_BUCKET, false);
    }

    @Override
    public Result uploadVideo(MultipartFile file) {
        return doUpload(file, VIDEO_BUCKET);
    }

    /**
     * 生成派生图并上传；任何一步失败都回退成原图上传。
     * 头像只保留 192 方图，封面保留 480/960 两档，主地址返回 960。
     */
    private Result uploadDerived(MultipartFile file, String bucketName, boolean avatar) {
        String ext = extOf(file.getOriginalFilename());
        if (imageProperties.isEnabled()) {
            try {
                byte[] source = file.getBytes();
                String base = UUID.randomUUID().toString();
                if (avatar) {
                    String avatarKey = base + "_" + imageProperties.getAvatarSize() + ".webp";
                    putBytes(bucketName, avatarKey, imageProcessService.avatar(source, ext), WEBP_CONTENT_TYPE);
                    return Result.ok(publicUrl(bucketName, avatarKey));
                }
                String largeKey = base + ImageUrls.LARGE_SUFFIX;
                String thumbKey = base + ImageUrls.THUMB_SUFFIX;
                putBytes(bucketName, largeKey, imageProcessService.coverLarge(source, ext), WEBP_CONTENT_TYPE);
                putBytes(bucketName, thumbKey, imageProcessService.coverThumb(source, ext), WEBP_CONTENT_TYPE);
                return Result.ok(publicUrl(bucketName, largeKey));
            } catch (Exception e) {
                log.warn("图片派生失败，回退原图：bucket={}, file={}", bucketName, file.getOriginalFilename(), e);
            }
        }
        return doUpload(file, bucketName);
    }

    /** 视频等大文件走流式上传，避免整文件读进内存 */
    private Result doUpload(MultipartFile file, String bucketName) {
        String ext = extOf(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + ext;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(in, file.getSize(), -1)
                            .contentType(CONTENT_TYPES.getOrDefault(ext, "application/octet-stream"))
                            .headers(Map.of("Cache-Control", IMMUTABLE_CACHE))
                            .build()
            );
        } catch (Exception e) {
            log.error("上传文件失败：bucket={}, object={}", bucketName, objectName, e);
            return Result.fail(APP_SERVER_ERROR.getCode(), "上传文件失败！");
        }
        return Result.ok(publicUrl(bucketName, objectName));
    }

    /** 派生图是内存中的字节，直接写对象存储 */
    private void putBytes(String bucketName, String objectName, byte[] data, String contentType) throws Exception {
        try (InputStream in = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(in, data.length, -1)
                            .contentType(contentType)
                            .headers(Map.of("Cache-Control", IMMUTABLE_CACHE))
                            .build()
            );
        }
    }

    /** 图片校验：后缀 + Content-Type + 体积；不合格直接拒绝，不写对象存储 */
    private void validateImage(MultipartFile file, long maxBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new VideoException(PARAM_ERROR.getCode(), "请选择" + label + "文件");
        }
        String ext = extOf(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!IMAGE_EXTS.contains(ext) || (contentType != null && !contentType.startsWith("image/"))) {
            throw new VideoException(PARAM_ERROR.getCode(), label + "只支持 jpg/png/webp/gif 图片");
        }
        if (file.getSize() > maxBytes) {
            throw new VideoException(PARAM_ERROR.getCode(),
                    label + "不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }
    }

    /** 公开读策略下的对象地址 */
    private String publicUrl(String bucketName, String objectName) {
        return String.format("%s/%s/%s", minIOProperties.getEndpoint(), bucketName, objectName);
    }

    /** 取小写后缀，无后缀返回空串 */
    private String extOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int index = originalFilename.lastIndexOf('.');
        return index < 0 ? "" : originalFilename.substring(index).toLowerCase(Locale.ROOT);
    }
}
