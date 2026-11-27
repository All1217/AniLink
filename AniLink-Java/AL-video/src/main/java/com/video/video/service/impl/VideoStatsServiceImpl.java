package com.video.video.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.video.model.entity.VideoStats;
import com.video.video.mapper.VideoStatsMapper;
import com.video.video.service.VideoStatsService;
import org.springframework.stereotype.Service;

@Service
public class VideoStatsServiceImpl extends ServiceImpl<VideoStatsMapper, VideoStats> implements VideoStatsService {
}
