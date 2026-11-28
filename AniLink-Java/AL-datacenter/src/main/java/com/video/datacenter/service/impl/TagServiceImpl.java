package com.video.datacenter.service.impl;

import com.video.api.user.TagInternalClient;
import com.video.api.user.dto.CommonTagsQueryVo;
import com.video.api.user.dto.TagUsersQueryVo;
import com.video.api.user.dto.UserTagVo;
import com.video.common.result.Result;
import com.video.datacenter.adapter.DanmakuQueryClient;
import com.video.datacenter.adapter.RecommendEngineClient;
import com.video.datacenter.service.TagService;
import com.video.datacenter.service.UserBriefService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.TagRankListVO;
import com.video.model.entity.Danmu;
import com.video.model.entity.UserInfo;
import com.video.model.enums.TagType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.video.common.result.ResultCodeEnum.PARAM_ERROR;

/**
 * 推荐/标签实现：跨域数据全部通过内部契约/公开列表取得后在内存组合。
 * <ul>
 *   <li>标签：al-user 的 user_tag 单表查询</li>
 *   <li>弹幕明细：al-danmaku 公开列表（已过滤 status=1）</li>
 *   <li>相似用户：Flask 引擎给 uid，al-user 批量补全资料</li>
 * </ul>
 */
@Slf4j
@Service
public class TagServiceImpl implements TagService {

    /** 标签排行要排除的画像标签（与拆分前 SQL 的白名单一致） */
    private static final Set<String> RANK_EXCLUDED_TAGS = Set.of("低质量弹幕倾向", "短平快弹幕");

    /** 标签排行只取前 8 个 */
    private static final int TAG_RANK_LIMIT = 8;

    /** 通用标签的类型码：选择该类型时用请求里的具体标签名 */
    private static final String COMMON_TAG_NAME = "通用";

    private final RecommendEngineClient engineClient;
    private final TagInternalClient tagInternalClient;
    private final DanmakuQueryClient danmakuQueryClient;
    private final UserBriefService userBriefService;

    public TagServiceImpl(RecommendEngineClient engineClient, TagInternalClient tagInternalClient,
                          DanmakuQueryClient danmakuQueryClient, UserBriefService userBriefService) {
        this.engineClient = engineClient;
        this.tagInternalClient = tagInternalClient;
        this.danmakuQueryClient = danmakuQueryClient;
        this.userBriefService = userBriefService;
    }

    @Override
    public Result<List<UserInfo>> getSimilarUsers(RecQueryVo queryVo) {
        if (queryVo == null || queryVo.getVid() == null || queryVo.getUid() == null) {
            return Result.ok(List.of());
        }
        // 引擎不可达时返回空列表，沿用旧实现「没有相似用户是正常结果」
        List<Long> uids = engineClient.similarUserIds(queryVo.getVid(), queryVo.getUid());
        if (uids.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(userBriefService.similarUsers(uids));
    }

    @Override
    public Result<List<Danmu>> filterUserByTags(RecQueryVo queryVo) {
        // 1.参数校验
        if (queryVo == null) {
            return Result.fail(PARAM_ERROR.getCode(), "请求参数不能为空");
        }
        Integer tagCode = queryVo.getTag();
        if (tagCode == null) {
            return Result.fail(PARAM_ERROR.getCode(), "标签代码不能为空");
        }
        // 2.标签代码转换；通用标签用请求里的具体标签名
        String tagName = convertCodeToTagName(tagCode);
        if (!StringUtils.hasText(tagName)) {
            return Result.fail(PARAM_ERROR.getCode(), "无效的标签代码");
        }
        if (COMMON_TAG_NAME.equals(tagName)) {
            tagName = queryVo.getTagName();
        }
        if (!StringUtils.hasText(tagName)) {
            return Result.ok(List.of());
        }
        // 3.取命中该标签的用户 id（weight>0 在 al-user 内部过滤）
        List<Long> uids = safeUidsByTag(tagName);
        if (uids.isEmpty()) {
            return Result.ok(List.of());
        }
        // 4.取该视频弹幕明细，按用户集合过滤并按出现时间点升序
        Set<Long> uidSet = new HashSet<>(uids);
        List<Danmu> res = safeDanmuList(queryVo.getVid()).stream()
                .filter(danmu -> danmu.getUid() != null && uidSet.contains(danmu.getUid()))
                .filter(danmu -> danmu.getStatus() == null || danmu.getStatus() == 1)
                .sorted(Comparator.comparing(Danmu::getTimePoint, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return Result.ok(res);
    }

    @Override
    public Result<List<String>> getCommonTags(RecQueryVo queryVo) {
        if (queryVo == null || queryVo.getUid() == null || queryVo.getVid() == null) {
            return Result.ok(List.of());
        }
        CommonTagsQueryVo query = new CommonTagsQueryVo();
        query.setUid(queryVo.getUid());
        // 旧接口把 vid 当另一个用户 id 用，这里保持一致语义
        query.setVid(queryVo.getVid());
        try {
            List<String> tags = tagInternalClient.commonTags(query);
            return Result.ok(tags == null ? List.of() : tags);
        } catch (Exception e) {
            log.warn("取共同标签失败，降级为空：uid={}, otherUid={}", queryVo.getUid(), queryVo.getVid(), e);
            return Result.ok(List.of());
        }
    }

    @Override
    public Result<List<TagRankListVO>> getTagRankList(Long vid) {
        // 1.取该视频弹幕明细；没有弹幕就没有标签排行
        List<Danmu> danmuList = safeDanmuList(vid);
        if (danmuList.isEmpty()) {
            return Result.ok(List.of());
        }
        // 2.批量取弹幕发送者的标签，按 uid 分组
        List<Long> uids = danmuList.stream().map(Danmu::getUid).filter(Objects::nonNull).distinct().toList();
        Map<Long, List<String>> tagsByUid = new HashMap<>();
        safeBatchTags(uids).forEach(tag -> {
            if (tag.getUid() != null && tag.getTagName() != null) {
                tagsByUid.computeIfAbsent(tag.getUid(), key -> new ArrayList<>()).add(tag.getTagName());
            }
        });
        // 3.按「标签 × 弹幕」计数（等价旧 SQL 的 COUNT(*)，一条弹幕命中一个标签计 1 次）
        Map<String, Integer> counter = new HashMap<>();
        for (Danmu danmu : danmuList) {
            for (String tagName : tagsByUid.getOrDefault(danmu.getUid(), List.of())) {
                if (RANK_EXCLUDED_TAGS.contains(tagName)) {
                    continue;
                }
                counter.merge(tagName, 1, Integer::sum);
            }
        }
        // 4.按出现次数降序取前 8
        List<TagRankListVO> rank = counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TAG_RANK_LIMIT)
                .map(entry -> {
                    TagRankListVO vo = new TagRankListVO();
                    vo.setName(entry.getKey());
                    vo.setValue(entry.getValue());
                    return vo;
                })
                .toList();
        return Result.ok(rank);
    }

    /** 标签代码转标签名；无效代码返回 null */
    private String convertCodeToTagName(Integer code) {
        for (TagType tagType : TagType.values()) {
            if (tagType.getCode().equals(code)) {
                return tagType.getTagName();
            }
        }
        return null;
    }

    /** 取命中标签的用户 id；al-user 不可用时降级为空 */
    private List<Long> safeUidsByTag(String tagName) {
        TagUsersQueryVo query = new TagUsersQueryVo();
        query.setTagName(tagName);
        try {
            List<Long> uids = tagInternalClient.uidsByTag(query);
            return uids == null ? List.of() : uids;
        } catch (Exception e) {
            log.warn("按标签取用户失败，降级为空：tag={}", tagName, e);
            return List.of();
        }
    }

    /** 批量取用户标签；al-user 不可用时降级为空 */
    private List<UserTagVo> safeBatchTags(List<Long> uids) {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }
        try {
            List<UserTagVo> tags = tagInternalClient.batch(uids);
            return tags == null ? List.of() : tags;
        } catch (Exception e) {
            log.warn("批量取用户标签失败，降级为空：size={}", uids.size(), e);
            return List.of();
        }
    }

    /** 取视频弹幕明细；al-danmaku 不可用时降级为空 */
    private List<Danmu> safeDanmuList(Long vid) {
        if (vid == null) {
            return List.of();
        }
        try {
            Result<List<Danmu>> result = danmakuQueryClient.listByVid(vid);
            if (result == null || result.getData() == null) {
                return List.of();
            }
            return result.getData();
        } catch (Exception e) {
            log.warn("取弹幕明细失败，降级为空：vid={}", vid, e);
            return List.of();
        }
    }
}
