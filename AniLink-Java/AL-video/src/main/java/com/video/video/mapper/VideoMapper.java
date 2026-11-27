package com.video.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.UserVideo;
import com.video.model.entity.Video;
import com.video.model.entity.VideoStats;
import com.video.video.vo.UserVideoQueryVo;
import com.video.video.vo.VideoQueryVo;
import com.video.video.vo.VideoVo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface VideoMapper extends BaseMapper<Video> {
    @Insert("insert into video (uid, title, type, auth, duration, mc_id, sc_id, tags, descr, status, visible, cover_url, cover_thumb_url, video_url, upload_date, delete_date) values (#{uid}, #{title}, #{type}, #{auth}, #{duration}, #{mcId}, #{scId}, #{tags}, #{descr}, #{status}, #{visible}, #{coverUrl}, #{coverThumbUrl}, #{videoUrl}, #{uploadDate}, #{deleteDate})")
    @Options(useGeneratedKeys = true, keyProperty = "vid", keyColumn = "vid")
    void insertOne(Video video);

    IPage<VideoVo> pageItem(IPage<VideoVo> page, VideoQueryVo queryVo);

    @Select("SELECT * from video where vid = #{vid} and visible = 0 and status = 1")
    Video getById(Long vid);

    List<VideoVo> batchSelect(List<Long> videoIds);

    /** 转码用：不过滤可见范围/审核状态，作者自己的私有稿件也要能转 */
    @Select("select * from video where vid = #{vid}")
    Video getRawById(Long vid);

    /** 待转码队列（存量数据默认为 0，服务启动时统一补跑） */
    @Select("select * from video where transcode_status = 0 and video_url is not null and video_url <> '' order by vid")
    List<Video> listPendingTranscode();

    /** 更新转码状态与失败原因（成功时清空 error） */
    @Update("update video set transcode_status = #{status}, transcode_error = #{error}, transcode_updated_at = now() where vid = #{vid}")
    int updateTranscodeStatus(@Param("vid") Long vid, @Param("status") int status, @Param("error") String error);

    /** 转码成功：写入 HLS 地址并复位状态 */
    @Update("update video set transcode_status = 2, hls_url = #{hlsUrl}, transcode_error = null, transcode_updated_at = now() where vid = #{vid}")
    int updateTranscodeDone(@Param("vid") Long vid, @Param("hlsUrl") String hlsUrl);

    /** 启动时把「转码中」复位成「待转码」，补跑上次中断的任务 */
    @Update("update video set transcode_status = 0, transcode_updated_at = now() where transcode_status = 1")
    int resetTranscoding();
}
