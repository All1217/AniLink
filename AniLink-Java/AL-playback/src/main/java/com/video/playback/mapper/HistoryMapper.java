package com.video.playback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.model.entity.BrowseHistory;
import com.video.playback.vo.HistoryQueryVo;
import com.video.playback.vo.HistoryVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 观看历史 Mapper：只操作本服务属主表 bh_video，视频/作者信息由内部接口补全。 */
public interface HistoryMapper extends BaseMapper<BrowseHistory> {
    IPage<HistoryVo> getHistoryPage(IPage<HistoryVo> page, @Param("queryVo") HistoryQueryVo queryVo);

    List<HistoryVo> listHistory(@Param("queryVo") HistoryQueryVo queryVo);

    void delete(@Param("uid") Long uid, @Param("vid") Long vid);

    @Select("select vid from bh_video where uid = #{uid}")
    List<Long> listVidsByUid(Long uid);

    @Delete("delete from bh_video where uid = #{uid}")
    void deleteByUid(Long uid);
}
