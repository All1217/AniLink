package com.video.web.admin.service;

import com.video.web.admin.vo.AdminBackfillResultVo;
import com.video.web.admin.vo.AdminCleanupResultVo;
import com.video.web.admin.vo.AdminObjectPageVo;
import com.video.web.admin.vo.AdminRefVo;

import java.util.List;

/** 管理端对象存储浏览、预览与带引用检查的删除。 */
public interface AdminStorageService {
    List<String> listBuckets();

    AdminObjectPageVo listObjects(String bucket, String prefix, String cursor, int size);

    String presign(String bucket, String key);

    /** 返回非空引用列表表示未删除；返回空列表表示删除成功。 */
    List<AdminRefVo> delete(String bucket, String key, Long operatorUid);

    /** 扫描当前桶/前缀下全部对象，删除无业务引用的对象。 */
    AdminCleanupResultVo cleanup(String bucket, String prefix, Long operatorUid);

    /** 为缺缩略图的存量封面生成 480w WebP 并写回，返回扫描/成功/跳过/失败数量。 */
    AdminBackfillResultVo backfillCoverThumbs(Long operatorUid);
}
