package com.video.web.main.controller;

import com.video.common.result.Result;
import com.video.model.entity.Danmu;
import com.video.model.entity.UserInfo;
import com.video.web.main.service.DanmuService;
import com.video.web.main.service.UserBriefService;
import com.video.web.main.service.UserTagService;
import com.video.web.main.vo.RecQueryVo;
import com.video.web.main.vo.TagRankListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * 弹幕域的标签/推荐接口：Phase 3 起弹幕发送与公开列表已迁 al-danmaku，
 * 这 4 个跨域接口（数据来自 user_tag 与 Flask 引擎）在 Phase 4 迁 al-datacenter 之前仍由 AL-web 提供，
 * 网关对这 4 条具体路径继续指向 webservice。
 */
@Tag(name = "弹幕标签/推荐接口（迁移期保留在 AL-web）")
@RestController
@RequestMapping("/main/danmu")
public class DanmuContorller {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DanmuService danmuService;
    @Autowired
    private UserBriefService userBriefService;
    @Autowired
    private UserTagService userTagService;

    @Operation(summary = "根据弹幕获取相似用户")
    @GetMapping("/getSimilarUsers")
    public Result<List<UserInfo>> getSimilarUsers(RecQueryVo queryVo) {
        // 向 Flask 服务发送 GET 请求，返回的视频 ID 列表
        String url = "http://localhost:5000/findSimilar/" + queryVo.getVid() + "/" + queryVo.getUid();
        List<Long> uids = restTemplate.getForObject(url, List.class);
        // 没有相似用户是正常结果，返回空列表即可（原实现返回 fail，前端会弹「暂无数据！」错误提示）
        if (uids == null || uids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(userBriefService.similarUsers(uids));
    }

    @Operation(summary = "根据指定标签过滤弹幕")
    @PostMapping("/filterUserByTags")
    public Result<List<Danmu>> filterUserByTags(@RequestBody RecQueryVo queryVo) {
        return danmuService.filterUserByTags(queryVo);
    }

    // 在这个接口中，uid和vid实际上均表示用户ID
    @Operation(summary = "查找两个用户的公共标签")
    @GetMapping("/getCommonTags")
    public Result<List<String>> getCommonTags(RecQueryVo queryVo) {
        return userTagService.getCommonTags(queryVo);
    }

    @Operation(summary = "获取出现频率前八的标签")
    @GetMapping("/getTagRankList")
    public Result<List<TagRankListVO>> getTagRankList(@RequestParam Long vid) {
        return userTagService.getTagRankList(vid);
    }
}
