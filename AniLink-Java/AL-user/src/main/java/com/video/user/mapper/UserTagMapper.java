package com.video.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.model.entity.UserTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户标签 Mapper：只访问属主表 user_tag（Phase 4 起 user_tag 归 al-user）。
 * 标签与弹幕、视频的聚合由数据中心 BFF 负责，这里只提供单表查询。
 */
public interface UserTagMapper extends BaseMapper<UserTag> {

    /** 取两个用户的共同标签名 */
    List<String> listCommonTagNames(@Param("uid") Long uid, @Param("otherUid") Long otherUid);

    /** 批量取用户标签行；不存在的用户不出现在结果里 */
    List<UserTag> listByUids(@Param("uids") List<Long> uids);

    /** 按标签名取权重 &gt; 0 的用户 id（与旧弹幕标签过滤 SQL 的条件一致） */
    List<Long> listUidsByTagName(@Param("tagName") String tagName);
}
