package com.video.web.admin.service;

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
}
