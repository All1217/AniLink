package com.video.web.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.web.admin.vo.AdminVideoQueryVo;
import com.video.web.admin.vo.AdminVideoVo;

/** 管理端稿件查询与逻辑状态操作。 */
public interface AdminVideoService {
    IPage<AdminVideoVo> page(AdminVideoQueryVo query);

    AdminVideoVo detail(Long vid);

    void updateStatus(Long vid, Integer status, Long operatorUid);

    void updateVisible(Long vid, Integer visible, Long operatorUid);
}
