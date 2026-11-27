package com.video.web.main.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户统计聚合（过渡期）。follow / video / video_stats 仍属 AL-web，
 * 供 al-user 通过 /internal/v1 读取；Phase 3/4 各域迁走后删除。
 */
public interface LegacyUserStatsMapper {

    @Select("select COUNT(*) from follow where followee_id = #{uid}")
    long calFansCount(@Param("uid") Long uid);

    @Select("select COUNT(*) from follow where follower_id = #{uid}")
    int calFollowCount(@Param("uid") Long uid);

    @Select("select ifnull(SUM(vs.cnt), 0) from " +
            "(select vid from video where uid = #{uid} and status = 1 and visible = 0) v " +
            "left join (select good, SUM(good) cnt, vid from video_stats group by vid) vs on v.vid = vs.vid")
    long calLikeCount(@Param("uid") Long uid);

    @Select("select ifnull(SUM(vs.cnt), 0) from " +
            "(select vid from video where uid = #{uid} and status = 1 and visible = 0) v " +
            "left join (select play, SUM(play) cnt, vid from video_stats group by vid) vs on v.vid = vs.vid")
    long calPlayCount(@Param("uid") Long uid);

    @Select("select COUNT(*) from video where uid = #{uid} and status = 1 and visible = 0")
    int calVideoCount(@Param("uid") Long uid);

    @Select("select ifnull(SUM(vs.cnt), 0) from " +
            "(select vid from video where uid = #{uid} and status = 1 and visible = 0) v " +
            "left join (select collect, SUM(collect) cnt, vid from video_stats group by vid) vs on v.vid = vs.vid")
    int calCollectCount(@Param("uid") Long uid);
}
