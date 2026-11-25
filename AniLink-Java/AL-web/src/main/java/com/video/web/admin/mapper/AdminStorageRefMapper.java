package com.video.web.admin.mapper;

import com.video.web.admin.vo.AdminRefVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 存储对象删除前的业务引用查询。 */
public interface AdminStorageRefMapper {

    /** 按完整 URL 查视频引用（video_url / cover_url / hls_url）。 */
    @Select("select vid as id, title as name, 'video' as type, " +
            "case when video_url = #{url} then 'video_url' " +
            "when cover_url = #{url} then 'cover_url' else 'hls_url' end as field " +
            "from video where video_url = #{url} or cover_url = #{url} or hls_url = #{url} limit 20")
    List<AdminRefVo> findVideoRefs(@Param("url") String url);

    /** 按完整 URL 查用户头像/背景引用。 */
    @Select("select uid as id, nickname as name, 'user' as type, " +
            "case when avatar = #{url} then 'avatar' else 'background' end as field " +
            "from user where avatar = #{url} or background = #{url} limit 20")
    List<AdminRefVo> findUserRefs(@Param("url") String url);

    /** HLS 目录引用：该 vid 仍有 hls_url 就视为整个目录被引用。 */
    @Select("select vid as id, title as name, 'video' as type, 'hls_url' as field " +
            "from video where vid = #{vid} and hls_url is not null and hls_url <> '' limit 1")
    AdminRefVo findHlsRef(@Param("vid") Long vid);
}
