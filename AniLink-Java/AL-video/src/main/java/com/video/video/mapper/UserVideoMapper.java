package com.video.video.mapper;

import com.video.api.video.dto.VideoDailyStatsQueryVo;
import com.video.api.video.dto.VideoDailyStatsVo;
import com.video.model.entity.UserVideo;
import com.video.model.entity.VideoStats;
import com.video.video.vo.UserVideoQueryVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface UserVideoMapper {
    @Select("select * from user_video where uid = #{uid} and vid = #{vid}")
    UserVideo selectActionById(Long uid, Long vid);

    @Insert("insert into user_video (uid, vid, play, love, unlove, coin, collect, play_time, love_time, coin_time) " +
            "values (#{uid}, #{vid}, #{play}, #{love}, #{unlove}, #{coin}, #{collect}, #{playTime}, #{loveTime}, #{coinTime})")
    void insertAction(UserVideo userVideo);

    void updateAction(UserVideo userVideo);

    VideoStats getStatsBySum(UserVideoQueryVo userVideoQueryVo);

    @Select("select * from user_video where vid = #{vid} and uid = #{uid}")
    UserVideo getInterActionStats(Long uid, Long vid);

    void batchUpdateStats(List<UserVideo> list);

    /**
     * 按 UP 主与时间范围聚合每日播放/粉丝播放/点赞。
     * 只连属主表 video 与 user_video；粉丝集合由调用方传入，不 JOIN follow。
     */
    List<VideoDailyStatsVo> dailyStats(@Param("query") VideoDailyStatsQueryVo query,
                                       @Param("beginTime") LocalDateTime beginTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 收藏事件幂等 upsert：行不存在则插入，已存在则覆盖收藏标记。
     * 取消收藏只把 collect 置 0，不改写收藏时间。
     */
    @Insert("insert into user_video (uid, vid, play, love, unlove, coin, collect, collect_time) " +
            "values (#{uid}, #{vid}, 0, 0, 0, 0, #{collect}, #{collectTime}) " +
            "on duplicate key update collect = values(collect), " +
            "collect_time = case when values(collect) = 1 then values(collect_time) else collect_time end")
    void upsertCollect(@Param("uid") Long uid, @Param("vid") Long vid,
                       @Param("collect") Integer collect, @Param("collectTime") Date collectTime);
}
