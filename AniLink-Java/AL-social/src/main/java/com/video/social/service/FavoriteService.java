package com.video.social.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.Favorite;
import com.video.model.entity.UserVideo;
import com.video.social.vo.BatchCollectVo;
import com.video.social.vo.BatchUpdateVo;
import com.video.social.vo.UserVideoQueryVo;
import com.video.social.vo.VideoFavoriteVo;
import com.video.social.vo.VideoVo;

import java.util.List;

/** 收藏夹域服务。 */
public interface FavoriteService {

    /** 新增收藏夹 */
    void insertOne(Favorite favorite);

    /** 查询用户对视频的收藏状态（1 已收藏，0 未收藏） */
    UserVideo getInterActionFavorite(UserVideoQueryVo userVideoQueryVo);

    /** 分页查用户收藏夹 */
    IPage<Favorite> getFavoriteListPage(IPage<Favorite> page, Long uid);

    /** 取收藏夹内视频信息（沿用旧响应结构） */
    List<VideoVo> getVideoByFavor(long fid);

    /** 分页查收藏夹内的视频（视频信息由 al-video 批量补全） */
    IPage<VideoFavoriteVo> getVideoFavoritePage(IPage<Favorite> page, long fid, String sortField);

    /** 修改收藏夹 */
    void update(Favorite favorite);

    /** 批量移动视频到新收藏夹 */
    Integer batchMove(BatchUpdateVo batchUpdateVo);

    /** 批量复制视频到新收藏夹 */
    Integer batchCopy(BatchUpdateVo batchUpdateVo);

    /** 查询视频存在于该用户的哪些收藏夹 */
    List<Long> getFVByUidAndVid(long uid, long vid);

    /** 将一个视频收藏到一个或多个收藏夹 */
    void collect(BatchCollectVo batchCollectVo);

    /** 将一个或多个视频从收藏夹移除 */
    void unCollect(BatchUpdateVo batchUpdateVo);

    /** 确保用户默认收藏夹存在（注册流程幂等调用），已存在返回 false */
    boolean ensureDefaultFavorite(Long uid);
}
