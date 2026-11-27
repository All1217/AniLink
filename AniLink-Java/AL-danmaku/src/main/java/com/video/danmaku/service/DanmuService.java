package com.video.danmaku.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.video.model.entity.Danmu;

/** 弹幕域业务接口：发送弹幕（公开列表走 MyBatis-Plus 通用查询）。 */
public interface DanmuService extends IService<Danmu> {
    void insertOne(Danmu danmu);
}
