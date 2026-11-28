package com.video.datacenter.service.impl;

import com.video.api.social.SocialInternalClient;
import com.video.api.video.VideoInternalClient;
import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.common.constant.Constant;
import com.video.datacenter.service.DataCenterService;
import com.video.datacenter.vo.DataResVo;
import com.video.model.entity.HotSearch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 数据中心统计实现：视频日汇总来自 al-video 内部契约，粉丝 id 集合来自 al-social，
 * 本服务只在内存拼装「本期 / 上一周期」的序列，不 JOIN 任何域的表。
 */
@Slf4j
@Service
public class DataCenterServiceImpl implements DataCenterService {

    /** 单次查询最大天数，防止异常入参拉出超长响应 */
    private static final int MAX_DAYS = 366;

    private final VideoInternalClient videoInternalClient;
    private final SocialInternalClient socialInternalClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public DataCenterServiceImpl(VideoInternalClient videoInternalClient,
                                 SocialInternalClient socialInternalClient,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.videoInternalClient = videoInternalClient;
        this.socialInternalClient = socialInternalClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public DataResVo getTotalPlay(Long uid, LocalDate begin, LocalDate end) {
        // 1.归一日期窗口并取「上一周期」起点
        DateWindow window = DateWindow.of(begin, end);
        if (window == null) {
            return emptyPlayResult();
        }
        // 2.取粉丝 id 集合，交给 al-video 拆粉丝播放量（视频域不 JOIN follow）
        List<Long> fansUids = safeFansIds(uid);
        // 3.一次取整段（上周期起点~本周期终点）的逐日汇总，再按日期切分
        Map<LocalDate, VideoDailyStatsVo> daily = safeDailyStats(uid, window, fansUids);
        DataResVo res = new DataResVo();
        res.setMainRes(pick(daily, window.currentDates(), VideoDailyStatsVo::getPlay));
        res.setSubRes(pick(daily, window.currentDates(), VideoDailyStatsVo::getFansPlay));
        res.setLastMainRes(pick(daily, window.previousDates(), VideoDailyStatsVo::getPlay));
        res.setLastSubRes(pick(daily, window.previousDates(), VideoDailyStatsVo::getFansPlay));
        return res;
    }

    @Override
    public DataResVo getLike(Long uid, LocalDate begin, LocalDate end) {
        // 1.归一日期窗口
        DateWindow window = DateWindow.of(begin, end);
        if (window == null) {
            return emptyLikeResult();
        }
        // 2.点赞不需要粉丝信息，直接取逐日汇总
        Map<LocalDate, VideoDailyStatsVo> daily = safeDailyStats(uid, window, List.of());
        DataResVo res = new DataResVo();
        res.setMainRes(pick(daily, window.currentDates(), VideoDailyStatsVo::getLike));
        res.setLastMainRes(pick(daily, window.previousDates(), VideoDailyStatsVo::getLike));
        // 旧接口的点赞结果不返回粉丝维度，保持 subRes/lastSubRes 为空
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<HotSearch> getHotSearch() {
        List<HotSearch> res = new ArrayList<>();
        try {
            // 引擎把 B 站 trending 的 JSON 写入 hotSearch 键，这里沿用旧的 Map#get("list") 读取方式
            Map<String, Object> json = (Map<String, Object>) redisTemplate.opsForValue().get(Constant.MAIN_HOT_SEARCH);
            if (json == null) {
                return res;
            }
            Object list = json.get("list");
            if (list instanceof List<?> records) {
                res = (List<HotSearch>) records;
            }
        } catch (Exception e) {
            log.warn("读取热搜缓存失败", e);
        }
        return res;
    }

    /** 粉丝 id 集合：社交域不可用时降级为空，粉丝播放量全部按 0 处理 */
    private List<Long> safeFansIds(Long uid) {
        if (uid == null) {
            return List.of();
        }
        try {
            List<Long> fans = socialInternalClient.fansIds(uid);
            return fans == null ? List.of() : fans;
        } catch (Exception e) {
            log.warn("取粉丝 id 集合失败，粉丝播放量按 0 处理：uid={}", uid, e);
            return List.of();
        }
    }

    /** 逐日汇总：视频域不可用时降级为空 Map，结果序列按 0 补齐 */
    private Map<LocalDate, VideoDailyStatsVo> safeDailyStats(Long uid, DateWindow window, List<Long> fansUids) {
        Map<LocalDate, VideoDailyStatsVo> byDate = new HashMap<>();
        if (uid == null) {
            return byDate;
        }
        VideoDailyStatsQueryVo query = new VideoDailyStatsQueryVo();
        query.setUid(uid);
        query.setBegin(window.previousFrom());
        query.setEnd(window.end());
        query.setFansUids(fansUids);
        try {
            List<VideoDailyStatsVo> rows = videoInternalClient.dailyStats(query);
            if (rows != null) {
                rows.stream().filter(row -> row.getDate() != null).forEach(row -> byDate.put(row.getDate(), row));
            }
        } catch (Exception e) {
            log.warn("取视频日汇总失败，本轮按 0 处理：uid={}", uid, e);
        }
        return byDate;
    }

    /** 按日期列表取指标，缺失日期补 0 */
    private List<Long> pick(Map<LocalDate, VideoDailyStatsVo> daily, List<LocalDate> dates,
                            Function<VideoDailyStatsVo, Long> getter) {
        List<Long> result = new ArrayList<>(dates.size());
        dates.forEach(date -> {
            VideoDailyStatsVo row = daily.get(date);
            Long value = row == null ? null : getter.apply(row);
            result.add(value == null ? 0L : value);
        });
        return result;
    }

    /** 空结果：日期非法时不返回 null 字段，保持前端等长序列可渲染 */
    private DataResVo emptyPlayResult() {
        DataResVo res = new DataResVo();
        res.setMainRes(List.of());
        res.setSubRes(List.of());
        res.setLastMainRes(List.of());
        res.setLastSubRes(List.of());
        return res;
    }

    /** 点赞空结果：只填本期/上期主序列 */
    private DataResVo emptyLikeResult() {
        DataResVo res = new DataResVo();
        res.setMainRes(List.of());
        res.setLastMainRes(List.of());
        return res;
    }

    /** 日期窗口：本周期 [begin, end] 与紧邻的等长上一周期，日期均按升序 */
    private record DateWindow(LocalDate begin, LocalDate end, List<LocalDate> currentDates,
                              List<LocalDate> previousDates) {

        /** 归一化入参：begin>end 时交换；空值返回 null，超长范围截断 */
        static DateWindow of(LocalDate begin, LocalDate end) {
            if (begin == null || end == null) {
                return null;
            }
            if (begin.isAfter(end)) {
                LocalDate temp = begin;
                begin = end;
                end = temp;
            }
            long days = ChronoUnit.DAYS.between(begin, end) + 1;
            if (days > MAX_DAYS) {
                log.warn("数据中心查询范围过大，已截断为 {} 天：begin={}, end={}", MAX_DAYS, begin, end);
                end = begin.plusDays(MAX_DAYS - 1L);
                days = MAX_DAYS;
            }
            List<LocalDate> current = dates(begin, end);
            List<LocalDate> previous = dates(begin.minusDays(days), begin.minusDays(1));
            return new DateWindow(begin, end, current, previous);
        }

        /** 上一周期起始日期：本期天数相同，紧邻本期之前 */
        LocalDate previousFrom() {
            return begin.minusDays(currentDates.size());
        }

        /** 生成 [from, to] 的升序日期列表 */
        private static List<LocalDate> dates(LocalDate from, LocalDate to) {
            List<LocalDate> result = new ArrayList<>();
            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                result.add(date);
            }
            return result;
        }
    }
}
