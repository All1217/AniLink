package com.video.playback.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.video.api.user.dto.UserBriefVo;
import com.video.api.video.dto.VideoBriefVo;
import com.video.common.login.LoginUser;
import com.video.common.login.LoginUserHolder;
import com.video.common.utils.StringUtils;
import com.video.model.dto.web.VideoRecordFormDTO;
import com.video.model.entity.BrowseHistory;
import com.video.playback.mapper.HistoryMapper;
import com.video.playback.service.HistoryService;
import com.video.playback.service.UserBriefService;
import com.video.playback.service.VideoBriefService;
import com.video.playback.task.VideoRecordCache;
import com.video.playback.task.VideoRecordDelayQueue;
import com.video.playback.vo.HistoryQueryVo;
import com.video.playback.vo.HistoryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 观看历史与播放进度实现：bh_video 归本服务，视频/作者信息通过内部批量接口补全。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl extends ServiceImpl<HistoryMapper, BrowseHistory> implements HistoryService {

    private final HistoryMapper historyMapper;
    private final VideoRecordCache recordCache;
    private final VideoRecordDelayQueue delayQueue;
    private final UserBriefService userBriefService;
    private final VideoBriefService videoBriefService;

    /** 新增历史记录（兼容旧前端 /main/history/add）：uid 以登录身份为准，已存在时只刷新观看时间 */
    @Override
    public void insertOne(BrowseHistory browseHistory) {
        if (browseHistory == null || browseHistory.getVid() == null) {
            return;
        }
        browseHistory.setUid(currentUid(browseHistory.getUid()));
        if (browseHistory.getViewTime() == null) {
            // view_time 列 NOT NULL 且无默认值，缺省时用当前时间兜底
            browseHistory.setViewTime(new java.util.Date());
        }
        try {
            // 1.首次观看：直接插入；view_time/duration 缺失时由数据库默认值兜底
            historyMapper.insert(browseHistory);
        } catch (DuplicateKeyException e) {
            // 2.已存在（唯一键 uid+vid）：与原 upsert 语义一致，只刷新观看时间
            BrowseHistory exists = historyMapper.selectOne(Wrappers.<BrowseHistory>lambdaQuery()
                    .eq(BrowseHistory::getVid, browseHistory.getVid())
                    .eq(BrowseHistory::getUid, browseHistory.getUid()));
            if (exists == null) {
                throw e;
            }
            BrowseHistory update = new BrowseHistory();
            update.setId(exists.getId());
            update.setViewTime(browseHistory.getViewTime());
            historyMapper.updateById(update);
        }
    }

    /** 历史分页：SQL 只查 bh_video，视频摘要与作者昵称批量补全 */
    @Override
    public IPage<HistoryVo> getHistoryPage(IPage<HistoryVo> page, HistoryQueryVo queryVo) {
        Long uid = currentUid(queryVo.getUid());
        queryVo.setUid(uid);
        // 1.按时长/关键词筛选依赖视频信息，本服务不持有 video 表，改为全量补全后内存过滤分页
        if (needVideoFilter(queryVo)) {
            return getHistoryPageByVideoFilter(page, queryVo);
        }
        // 2.常规场景直接 SQL 分页，再批量补全视频与作者信息
        IPage<HistoryVo> result = historyMapper.getHistoryPage(page, queryVo);
        enrich(result.getRecords());
        return result;
    }

    /** 按时长/关键词筛选：先取该用户全部历史，补全视频信息后过滤，再按页截取 */
    private IPage<HistoryVo> getHistoryPageByVideoFilter(IPage<HistoryVo> page, HistoryQueryVo queryVo) {
        List<HistoryVo> records = historyMapper.listHistory(queryVo);
        enrich(records);
        // 1.按视频时长与标题关键词过滤（video.duration 缺失的记录与旧 SQL 语义一致地排除）
        List<HistoryVo> filtered = records.stream()
                .filter(item -> matchesDuration(item, queryVo))
                .filter(item -> matchesKeyword(item, queryVo))
                .toList();
        // 2.内存分页：total 取过滤后的真实条数
        long from = Math.max(0, (page.getCurrent() - 1) * page.getSize());
        int start = (int) Math.min(from, filtered.size());
        int end = (int) Math.min(start + page.getSize(), filtered.size());
        page.setTotal(filtered.size());
        page.setRecords(new ArrayList<>(filtered.subList(start, end)));
        return page;
    }

    /** 是否需要视频维度的筛选项 */
    private boolean needVideoFilter(HistoryQueryVo queryVo) {
        return queryVo.getStartDuration() != null
                || queryVo.getEndDuration() != null
                || StringUtils.isNotBlank(queryVo.getKeyword());
    }

    /** 视频时长是否落在筛选区间 */
    private boolean matchesDuration(HistoryVo item, HistoryQueryVo queryVo) {
        Double duration = item.getVideoDuration();
        if (duration == null) {
            return false;
        }
        if (queryVo.getStartDuration() != null && duration < queryVo.getStartDuration()) {
            return false;
        }
        return queryVo.getEndDuration() == null || duration <= queryVo.getEndDuration();
    }

    /** 标题是否包含关键词（未传关键词视为命中） */
    private boolean matchesKeyword(HistoryVo item, HistoryQueryVo queryVo) {
        if (StringUtils.isBlank(queryVo.getKeyword())) {
            return true;
        }
        return item.getTitle() != null && item.getTitle().contains(queryVo.getKeyword());
    }

    /** 批量补全视频摘要（al-video）与作者昵称（al-user） */
    private void enrich(List<HistoryVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 1.批量取视频摘要，补全标题/封面/时长/作者 uid
        Map<Long, VideoBriefVo> videos = videoBriefService.byVids(
                records.stream().map(HistoryVo::getVid).toList());
        records.forEach(history -> {
            VideoBriefVo video = videos.get(history.getVid());
            if (video == null) {
                return;
            }
            history.setTitle(video.getTitle());
            history.setVideoDuration(video.getDuration());
            history.setCoverUrl(video.getCoverUrl());
            // 缩略图缺失时回退主图，与旧 SQL 的 coalesce 语义一致
            history.setCoverThumbUrl(video.getCoverThumbUrl() != null
                    ? video.getCoverThumbUrl() : video.getCoverUrl());
            history.setAuthorUid(video.getUid());
        });
        // 2.批量取作者昵称
        Map<Long, UserBriefVo> users = userBriefService.byUids(
                records.stream().map(HistoryVo::getAuthorUid).filter(java.util.Objects::nonNull).toList());
        records.forEach(history -> {
            UserBriefVo user = users.get(history.getAuthorUid());
            history.setNickname(user == null ? null : user.getNickname());
        });
    }

    /** 批量删除历史：删库后同步清理读缓存与待落库任务，避免被延迟任务复活 */
    @Override
    public void deleteByVids(HistoryQueryVo queryVo) {
        Long uid = currentUid(queryVo.getUid());
        if (queryVo.getVids() == null || queryVo.getVids().isEmpty()) {
            return;
        }
        queryVo.getVids().forEach(vid -> deleteOne(uid, vid));
    }

    /** 删除单条历史 */
    @Override
    public void deleteSingle(HistoryQueryVo queryVo) {
        if (queryVo.getVids() == null || queryVo.getVids().isEmpty()) {
            return;
        }
        deleteOne(currentUid(queryVo.getUid()), queryVo.getVids().get(0));
    }

    /** 清空历史：先取该用户全部 vid 用于清理缓存，再整批删除 */
    @Override
    public void deleteByUid(HistoryQueryVo queryVo) {
        Long uid = currentUid(queryVo.getUid());
        List<Long> vids = historyMapper.listVidsByUid(uid);
        historyMapper.deleteByUid(uid);
        if (vids != null) {
            vids.forEach(vid -> cleanCache(vid, uid));
        }
    }

    /** 删除一条记录并清理对应缓存 */
    private void deleteOne(Long uid, Long vid) {
        historyMapper.delete(uid, vid);
        cleanCache(vid, uid);
    }

    /** 清理单条记录的读缓存与待落库任务 */
    private void cleanCache(Long vid, Long uid) {
        recordCache.clean(vid, uid);
        delayQueue.remove(vid, uid);
    }

    /** 上报播放进度：首次插入直接落库，之后写入 10 秒合并窗口由延迟队列统一落库 */
    @Override
    public boolean addVideoRecord(VideoRecordFormDTO formDTO) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null || formDTO == null || formDTO.getVid() == null) {
            return false;
        }
        Long uid = loginUser.getUserId();
        // 1.查询旧的观看记录（读缓存优先，未命中回源数据库）
        BrowseHistory old = queryOldRecord(formDTO.getVid(), uid);
        // 2.不存在则新增，存在则进入延迟落库
        if (old == null) {
            return insertRecord(formDTO, uid);
        }
        updateRecord(formDTO, uid, old.getId());
        return true;
    }

    /** 首次播放：直接落库；并发下另一个请求可能已经插入，撞唯一键时退化成更新 */
    private boolean insertRecord(VideoRecordFormDTO formDTO, Long uid) {
        BrowseHistory record = new BrowseHistory();
        record.setVid(formDTO.getVid());
        record.setUid(uid);
        record.setViewTime(Timestamp.valueOf(commitTime(formDTO)));
        record.setDuration(formDTO.getMoment());
        try {
            historyMapper.insert(record);
            // 首次播放已经落库，同时回填读缓存，下一次上报不必再查库
            recordCache.write(record);
            return false;
        } catch (DuplicateKeyException e) {
            // 快速切视频 / 多标签页同时首次上报：唯一键 (uid, vid) 已被写入，按已存在处理
            BrowseHistory exists = historyMapper.selectOne(Wrappers.<BrowseHistory>lambdaQuery()
                    .eq(BrowseHistory::getVid, formDTO.getVid())
                    .eq(BrowseHistory::getUid, uid));
            if (exists == null) {
                throw e;
            }
            updateRecord(formDTO, uid, exists.getId());
            return true;
        }
    }

    /** 已有记录：只写读缓存与延迟队列，窗口结束后统一落库，避免每次上报都打数据库 */
    private void updateRecord(VideoRecordFormDTO formDTO, Long uid, Long id) {
        BrowseHistory record = new BrowseHistory();
        record.setId(id);
        record.setVid(formDTO.getVid());
        record.setUid(uid);
        record.setDuration(formDTO.getMoment());
        delayQueue.submit(record);
        recordCache.write(record);
    }

    /** 查询旧记录：读缓存优先，未命中回源数据库并回填缓存 */
    private BrowseHistory queryOldRecord(Long vid, Long uid) {
        BrowseHistory record = recordCache.read(vid, uid);
        if (record != null) {
            return record;
        }
        record = historyMapper.selectOne(Wrappers.<BrowseHistory>lambdaQuery()
                .eq(BrowseHistory::getVid, vid)
                .eq(BrowseHistory::getUid, uid));
        if (record != null) {
            recordCache.write(record);
        }
        return record;
    }

    /** 上报时间缺失时按当前时间兜底，避免 view_time 为空写库失败 */
    private LocalDateTime commitTime(VideoRecordFormDTO formDTO) {
        return formDTO.getCommitTime() == null ? LocalDateTime.now() : formDTO.getCommitTime();
    }

    /** 登录身份优先（design D7：写操作不以请求参数中的 uid 为依据），缺失时回退传入值 */
    private Long currentUid(Long fallback) {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        return loginUser == null ? fallback : loginUser.getUserId();
    }
}
