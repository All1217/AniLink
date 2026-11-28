package com.video.datacenter.service.impl;

import com.video.datacenter.adapter.RecommendEngineClient;
import com.video.datacenter.service.RecommendService;
import com.video.datacenter.service.UserBriefService;
import com.video.datacenter.service.VideoBriefService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.VideoVo;
import com.video.api.user.dto.UserBriefVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** 实时推荐实现：Flask 只给 id，视频与作者信息走其他域内部契约批量补全。 */
@Slf4j
@Service
public class RecommendServiceImpl implements RecommendService {

    private final RecommendEngineClient engineClient;
    private final VideoBriefService videoBriefService;
    private final UserBriefService userBriefService;

    public RecommendServiceImpl(RecommendEngineClient engineClient,
                                VideoBriefService videoBriefService,
                                UserBriefService userBriefService) {
        this.engineClient = engineClient;
        this.videoBriefService = videoBriefService;
        this.userBriefService = userBriefService;
    }

    @Override
    public List<VideoVo> getRealTimeRecommend(RecQueryVo queryVo) {
        // 1.向 Flask 取推荐视频 id；引擎不可达时为空列表，接口仍返回成功
        List<Long> videoIds = engineClient.recommendVideoIds(
                queryVo == null ? null : queryVo.getUid(),
                queryVo == null ? null : queryVo.getCount());
        if (videoIds.isEmpty()) {
            return List.of();
        }
        // 2.批量补全视频摘要与统计，保持旧实现的播放量升序
        List<VideoVo> res = videoBriefService.legacyVideosInPlayOrder(videoIds);
        // 3.作者昵称走 al-user 批量接口，避免跨域 JOIN user
        fillNicknames(res);
        log.info("实时推荐结果条数：{}", res.size());
        return res;
    }

    /** 作者昵称改由 al-user 批量补全，避免跨域 JOIN user */
    private void fillNicknames(List<VideoVo> records) {
        if (records.isEmpty()) {
            return;
        }
        Map<Long, UserBriefVo> users = userBriefService.byUids(
                records.stream().map(VideoVo::getUid).toList());
        records.forEach(video -> {
            UserBriefVo user = users.get(video.getUid());
            video.setNickname(user == null ? null : user.getNickname());
        });
    }
}
