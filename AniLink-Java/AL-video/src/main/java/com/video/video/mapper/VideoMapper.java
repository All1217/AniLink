package com.video.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.api.video.dto.AdminCoverThumbVo;
import com.video.api.video.dto.AdminVideoRefVo;
import com.video.api.video.dto.UserVideoStatsVo;
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

import java.util.Date;
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

    /**
     * 按 UP 主汇总名下已过审可见稿件的获赞/播放/投稿/收藏。
     * 稿件归属看 video.uid；user_video.uid 是观看者（跨域用户 id），不能当作者过滤。
     */
    UserVideoStatsVo userVideoStats(@Param("uid") Long uid);

    /** 管理端：按完整 URL 查稿件引用（video_url / cover_url / cover_thumb_url / hls_url） */
    @Select("select vid as id, title as name, 'video' as type, " +
            "case when video_url = #{url} then 'video_url' " +
            "when cover_url = #{url} then 'cover_url' " +
            "when cover_thumb_url = #{url} then 'cover_thumb_url' else 'hls_url' end as field " +
            "from video where video_url = #{url} or cover_url = #{url} " +
            "or cover_thumb_url = #{url} or hls_url = #{url} limit 20")
    List<AdminVideoRefVo> findVideoRefs(@Param("url") String url);

    /** 管理端：HLS 目录引用，该 vid 仍有 hls_url 就视为整个目录被引用 */
    @Select("select vid as id, title as name, 'video' as type, 'hls_url' as field " +
            "from video where vid = #{vid} and hls_url is not null and hls_url <> '' limit 1")
    AdminVideoRefVo findHlsRef(@Param("vid") Long vid);

    /** 管理端：按 vid 游标取缺少封面缩略图的稿件，只查空值天然幂等 */
    @Select("select vid, cover_url from video " +
            "where cover_thumb_url is null and cover_url is not null and cover_url <> '' " +
            "and vid > #{afterVid} order by vid limit #{limit}")
    List<AdminCoverThumbVo> listMissingThumbs(@Param("afterVid") long afterVid, @Param("limit") int limit);

    /** 管理端：写回封面缩略图，空值判断避免并发回填互相覆盖 */
    @Update("update video set cover_thumb_url = #{thumbUrl} where vid = #{vid} and cover_thumb_url is null")
    int updateCoverThumb(@Param("vid") Long vid, @Param("thumbUrl") String thumbUrl);

    /** 管理端：更新审核/删除状态；下架时写入 delete_date，其余清空 */
    @Update("update video set status = #{status}, delete_date = #{deleteDate} where vid = #{vid}")
    int updateAdminStatus(@Param("vid") Long vid, @Param("status") Integer status,
                          @Param("deleteDate") Date deleteDate);

    /** 管理端：更新公开/仅自己可见 */
    @Update("update video set visible = #{visible} where vid = #{vid}")
    int updateAdminVisible(@Param("vid") Long vid, @Param("visible") Integer visible);
}
