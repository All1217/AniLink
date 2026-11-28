package com.video.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.admin.vo.AdminVideoQueryVo;
import com.video.admin.vo.AdminVideoVo;

/** 管理端稿件查询与逻辑状态操作。 */
public interface AdminVideoService {
    IPage<AdminVideoVo> page(AdminVideoQueryVo query);

    AdminVideoVo detail(Long vid);

    void updateStatus(Long vid, Integer status, Long operatorUid);

    void updateVisible(Long vid, Integer visible, Long operatorUid);
}
