package com.video.danmaku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.model.entity.Danmu;

/** 弹幕 Mapper：只访问 danmu 表，标签过滤等跨域聚合归 al-datacenter。 */
public interface DanmuMapper extends BaseMapper<Danmu> {
}
