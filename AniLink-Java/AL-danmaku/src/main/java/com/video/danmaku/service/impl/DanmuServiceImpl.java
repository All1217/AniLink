package com.video.danmaku.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.video.danmaku.mapper.DanmuMapper;
import com.video.danmaku.service.DanmuService;
import com.video.model.entity.Danmu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 弹幕域业务实现：id 归零交给 danmu 表自增，保持迁移前落库语义。 */
@Slf4j
@Service
public class DanmuServiceImpl extends ServiceImpl<DanmuMapper, Danmu> implements DanmuService {

    @Autowired
    private DanmuMapper danmuMapper;

    @Override
    public void insertOne(Danmu danmu) {
        log.info("danmu: {}", danmu);
        danmu.setId(0L);
        danmuMapper.insert(danmu);
    }
}
