package com.video.admin.mapper;

import com.video.admin.vo.AdminCoverBackfillVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 存量封面缩略图回填：按 vid 游标取缺缩略图的稿件。 */
public interface AdminCoverBackfillMapper {

    /** 只取还没有缩略图的稿件，天然幂等 */
    @Select("select vid, cover_url from video " +
            "where cover_thumb_url is null and cover_url is not null and cover_url <> '' " +
            "and vid > #{afterVid} order by vid limit #{limit}")
    List<AdminCoverBackfillVo> listMissingThumbs(@Param("afterVid") long afterVid, @Param("limit") int limit);

    /** 写回前再判一次空值，避免并发回填互相覆盖 */
    @Update("update video set cover_thumb_url = #{thumbUrl} where vid = #{vid} and cover_thumb_url is null")
    int updateThumb(@Param("vid") Long vid, @Param("thumbUrl") String thumbUrl);
}
