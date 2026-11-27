package com.video.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.Favorite;
import com.video.model.entity.FavoriteVideo;
import com.video.social.vo.VideoFavoriteVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 收藏夹 Mapper：只读写属主表 favorite、favorite_video。
 * 视频标题/封面/统计由 al-video 内部接口批量补全，禁止 JOIN video / video_stats。
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /** 统计某用户是否收藏了该视频（收藏状态查询，只查属主表） */
    @Select("select count(*) from favorite_video fv join favorite f on fv.fid = f.fid " +
            "where fv.vid = #{vid} and f.uid = #{uid} and fv.is_remove = 0 and f.is_delete = 0")
    int countCollected(@Param("uid") Long uid, @Param("vid") Long vid);

    /** 分页查询用户的收藏夹列表 */
    IPage<Favorite> getFavoriteListPage(IPage<Favorite> page, @Param("uid") Long uid);

    /** 取收藏夹内未移除的视频 ID */
    @Select("select vid from favorite_video where fid = #{fid} and is_remove = 0")
    List<Long> getVideoIds(@Param("fid") long fid);

    /** 分页查询收藏夹内的收藏关系（视频信息另行批量补全） */
    IPage<VideoFavoriteVo> getVideoFavoritePage(IPage<Favorite> page, @Param("fid") long fid);

    /** 收藏夹视频数减 */
    @Update("update favorite set count = count - #{size} where fid = #{fid}")
    void subCount(@Param("size") Integer size, @Param("fid") Long fid);

    /** 收藏夹视频数加 */
    @Update("update favorite set count = count + #{size} where fid = #{fid}")
    void addCount(@Param("size") Integer size, @Param("fid") Long fid);

    /** 新增收藏关系 */
    @Insert("INSERT INTO favorite_video (vid, fid, time, is_remove) values (#{vid}, #{fid}, #{time}, #{isRemove})")
    void batchInsertSv(FavoriteVideo favoriteVideo);

    /** 删除收藏关系 */
    @Delete("delete from favorite_video where fid = #{fid} and vid = #{vid}")
    void deleteFVById(@Param("fid") Long fid, @Param("vid") Long vid);

    /** 查询视频存在于该用户的哪些收藏夹 */
    List<Long> getFVByUidAndVid(@Param("uid") long uid, @Param("vid") long vid);

    /** 统计用户已有默认收藏夹数量，注册时幂等创建默认收藏夹 */
    @Select("select count(*) from favorite where uid = #{uid} and type = 1 and is_delete = 0")
    int countDefaultFavorite(@Param("uid") Long uid);

    /** 取收藏夹属主：取消收藏入参没有 uid，用 fid 反查 */
    @Select("select uid from favorite where fid = #{fid} and is_delete = 0")
    Long getFavoriteOwner(@Param("fid") Long fid);
}
