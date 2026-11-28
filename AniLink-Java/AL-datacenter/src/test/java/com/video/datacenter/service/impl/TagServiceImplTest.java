package com.video.datacenter.service.impl;

import com.video.api.user.TagInternalClient;
import com.video.api.user.dto.CommonTagsQueryVo;
import com.video.api.user.dto.UserTagVo;
import com.video.common.result.Result;
import com.video.datacenter.adapter.DanmakuQueryClient;
import com.video.datacenter.adapter.RecommendEngineClient;
import com.video.datacenter.service.UserBriefService;
import com.video.datacenter.vo.RecQueryVo;
import com.video.datacenter.vo.TagRankListVO;
import com.video.model.entity.Danmu;
import com.video.model.entity.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 推荐/标签接口：标签 × 弹幕在内存聚合、参数校验、跨域下游降级。 */
@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private RecommendEngineClient engineClient;
    @Mock
    private TagInternalClient tagInternalClient;
    @Mock
    private DanmakuQueryClient danmakuQueryClient;
    @Mock
    private UserBriefService userBriefService;
    @InjectMocks
    private TagServiceImpl tagService;

    @Test
    @DisplayName("相似用户：引擎 uid 列表按顺序补全用户资料")
    void similarUsersFillsProfiles() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setVid(11L);
        queryVo.setUid(7L);
        when(engineClient.similarUserIds(11L, 7L)).thenReturn(List.of(9L, 10L));
        UserInfo first = new UserInfo();
        first.setUid(9L);
        UserInfo second = new UserInfo();
        second.setUid(10L);
        when(userBriefService.similarUsers(List.of(9L, 10L))).thenReturn(List.of(first, second));

        Result<List<UserInfo>> result = tagService.getSimilarUsers(queryVo);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("相似用户：引擎不可达时返回空结果而不是失败")
    void similarUsersEmptyWhenEngineDown() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setVid(11L);
        queryVo.setUid(7L);
        when(engineClient.similarUserIds(11L, 7L)).thenReturn(List.of());

        Result<List<UserInfo>> result = tagService.getSimilarUsers(queryVo);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("标签过滤弹幕：只保留命中标签的用户并按时间点升序")
    void filterUserByTagsFiltersAndSorts() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setVid(11L);
        queryVo.setTag(0);
        when(tagInternalClient.uidsByTag(any())).thenReturn(List.of(2L));
        when(danmakuQueryClient.listByVid(11L)).thenReturn(Result.ok(List.of(
                danmu(1L, 1L, 5.0), danmu(2L, 2L, 2.0), danmu(3L, 2L, 1.0), danmu(4L, 3L, 0.0))));

        Result<List<Danmu>> result = tagService.filterUserByTags(queryVo);

        // 标签码 0 -> 高质量弹幕贡献者
        ArgumentCaptor<com.video.api.user.dto.TagUsersQueryVo> captor =
                ArgumentCaptor.forClass(com.video.api.user.dto.TagUsersQueryVo.class);
        verify(tagInternalClient).uidsByTag(captor.capture());
        assertEquals("高质量弹幕贡献者", captor.getValue().getTagName());
        assertEquals(List.of(3L, 2L), result.getData().stream().map(Danmu::getId).toList());
    }

    @Test
    @DisplayName("标签过滤弹幕：通用标签用请求里的具体标签名")
    void filterUserByCommonTagUsesRequestedName() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setVid(11L);
        queryVo.setTag(4);
        queryVo.setTagName("自定义标签");
        when(tagInternalClient.uidsByTag(any())).thenReturn(List.of());

        Result<List<Danmu>> result = tagService.filterUserByTags(queryVo);

        ArgumentCaptor<com.video.api.user.dto.TagUsersQueryVo> captor =
                ArgumentCaptor.forClass(com.video.api.user.dto.TagUsersQueryVo.class);
        verify(tagInternalClient).uidsByTag(captor.capture());
        assertEquals("自定义标签", captor.getValue().getTagName());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("标签过滤弹幕：参数非法返回 202，而不是查下游")
    void filterUserByTagsRejectsIllegalParams() {
        assertEquals(202, tagService.filterUserByTags(null).getCode());

        RecQueryVo noTag = new RecQueryVo();
        noTag.setVid(11L);
        assertEquals(202, tagService.filterUserByTags(noTag).getCode());

        RecQueryVo badCode = new RecQueryVo();
        badCode.setVid(11L);
        badCode.setTag(99);
        assertEquals(202, tagService.filterUserByTags(badCode).getCode());
        verify(tagInternalClient, never()).uidsByTag(any());
    }

    @Test
    @DisplayName("共同标签：uid 与 vid 两个用户 id 透传到 al-user")
    void commonTagsPassBothUserIds() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setUid(7L);
        queryVo.setVid(9L);
        when(tagInternalClient.commonTags(any())).thenReturn(List.of("干货贡献者", "通用"));

        Result<List<String>> result = tagService.getCommonTags(queryVo);

        ArgumentCaptor<CommonTagsQueryVo> captor = ArgumentCaptor.forClass(CommonTagsQueryVo.class);
        verify(tagInternalClient).commonTags(captor.capture());
        assertEquals(7L, captor.getValue().getUid());
        assertEquals(9L, captor.getValue().getVid());
        assertEquals(List.of("干货贡献者", "通用"), result.getData());
    }

    @Test
    @DisplayName("共同标签：al-user 不可用时降级为空列表")
    void commonTagsDegradesWhenUserDown() {
        RecQueryVo queryVo = new RecQueryVo();
        queryVo.setUid(7L);
        queryVo.setVid(9L);
        when(tagInternalClient.commonTags(any())).thenThrow(new RuntimeException("user down"));

        Result<List<String>> result = tagService.getCommonTags(queryVo);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("标签排行：按「标签 × 弹幕」计数，排除画像标签并降序")
    void tagRankAggregatesAndExcludes() {
        when(danmakuQueryClient.listByVid(11L)).thenReturn(Result.ok(List.of(
                danmu(1L, 1L, 1.0), danmu(2L, 1L, 2.0), danmu(3L, 2L, 3.0))));
        when(tagInternalClient.batch(any())).thenReturn(List.of(
                tag(1L, "标签A"), tag(1L, "标签B"), tag(2L, "标签A"), tag(2L, "低质量弹幕倾向")));

        Result<List<TagRankListVO>> result = tagService.getTagRankList(11L);

        List<TagRankListVO> rank = result.getData();
        // 标签A：3 条弹幕命中；标签B：2 条（uid1 的两条弹幕）；低质量弹幕倾向被排除
        assertEquals(2, rank.size());
        assertEquals("标签A", rank.get(0).getName());
        assertEquals(3, rank.get(0).getValue());
        assertEquals("标签B", rank.get(1).getName());
        assertEquals(2, rank.get(1).getValue());
    }

    @Test
    @DisplayName("标签排行：最多返回 8 个标签")
    void tagRankLimitedToEight() {
        when(danmakuQueryClient.listByVid(11L)).thenReturn(Result.ok(List.of(danmu(1L, 1L, 1.0))));
        List<UserTagVo> tags = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tags.add(tag(1L, "标签" + i));
        }
        when(tagInternalClient.batch(any())).thenReturn(tags);

        assertEquals(8, tagService.getTagRankList(11L).getData().size());
    }

    @Test
    @DisplayName("标签排行：弹幕域不可用时返回空列表")
    void tagRankDegradesWhenDanmakuDown() {
        when(danmakuQueryClient.listByVid(11L)).thenThrow(new RuntimeException("danmaku down"));

        Result<List<TagRankListVO>> result = tagService.getTagRankList(11L);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    /** 构造弹幕行 */
    private Danmu danmu(Long id, Long uid, Double timePoint) {
        Danmu danmu = new Danmu();
        danmu.setId(id);
        danmu.setUid(uid);
        danmu.setStatus(1);
        danmu.setTimePoint(timePoint);
        return danmu;
    }

    /** 构造跨服务标签行 */
    private UserTagVo tag(Long uid, String tagName) {
        UserTagVo vo = new UserTagVo();
        vo.setUid(uid);
        vo.setTagName(tagName);
        vo.setWeight(1.0);
        return vo;
    }
}
