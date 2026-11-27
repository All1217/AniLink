package com.video.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.model.entity.UserVideo;
import com.video.model.entity.VideoStats;
import com.video.video.vo.UserVideoQueryVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface VideoStatsMapper extends BaseMapper<VideoStats> {
    @Select("select * from user_video where uid=#{uid} and vid=#{vid}")
    UserVideo getInterActLike(UserVideoQueryVo userVideoQueryVo);

    @Select("select * from user_video where uid=#{uid} and vid=#{vid}")
    UserVideo getInterActionCoin(UserVideoQueryVo userVideoQueryVo);

    @Insert("insert into video_stats (vid, play, danmu, good, bad, coin, collect, share, comment) " +
            "VALUES (#{vid}, #{play}, #{danmu}, #{good}, #{bad}, #{coin}, #{collect}, #{share}, #{comment})")
    void insertOneStats(VideoStats videoStats);

    @Select("select * from video_stats where vid = #{vid}")
    VideoStats getVideoStatsById(Long vid);

    void updateVideoStats(VideoStats videoStats);

    void addVideoStats(VideoStats videoStats);

    @Select("select SUM(play) from user_video where vid = #{vid}")
    long calPlay(Long vid);

    @Select("select SUM(love) from user_video where vid = #{vid}")
    long calLike(Long vid);

    @Select("select SUM(coin) from user_video where vid = #{vid}")
    long calCoin(Long vid);

    @Select("select SUM(collect) from user_video where vid = #{vid}")
    long calCollect(Long vid);

    void batchAddStats(List<VideoStats> list);

    /** 收藏事件按增量更新视频收藏数，下限 0；返回 0 表示视频统计行不存在 */
    @Update("update video_stats set collect = GREATEST(IFNULL(collect, 0) + #{delta}, 0) where vid = #{vid}")
    int addCollect(@Param("vid") Long vid, @Param("delta") Integer delta);
}
