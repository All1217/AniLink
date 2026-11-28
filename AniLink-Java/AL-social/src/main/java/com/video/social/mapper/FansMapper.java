package com.video.social.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.api.social.dto.UserFollowStatsVo;
import com.video.model.entity.Follow;
import com.video.model.entity.UserInfo;
import com.video.social.vo.FollowQueryVo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 关注关系 Mapper：只读写属主表 follow。 */
public interface FansMapper {

    /** 新增关注关系 */
    @Insert("insert into follow (follower_id, followee_id, created_at) VALUES (#{followerId}, #{followeeId}, #{createdAt})")
    void insertOne(Follow follow);

    /** 取消关注 */
    @Delete("delete from follow where followee_id=#{followeeId} and follower_id=#{followerId}")
    void deleteById(Follow follow);

    /** 查询关注状态 */
    @Select("select * from follow where followee_id=#{followeeId} and follower_id=#{followerId}")
    Follow getFollow(Follow follow);

    /** 分页查询关注列表（返回 uid 与互相关注标记） */
    IPage<UserInfo> getFollowList(IPage<UserInfo> page, @Param("query") FollowQueryVo query);

    /** 分页查询粉丝列表（返回 uid 与互相关注标记） */
    IPage<UserInfo> getFansList(IPage<UserInfo> page, @Param("query") FollowQueryVo query);

    /** 取某用户的粉丝 id 集合：数据中心拆「粉丝播放量」用，只查属主表 follow */
    @Select("select follower_id from follow where followee_id = #{uid}")
    List<Long> listFansIds(@Param("uid") Long uid);

    /**
     * 取某用户的关注统计：粉丝数与被关注数都来自属主表 follow 的两个方向。
     * uid 是用户域 id（跨域引用），这里只做计数，不 JOIN user 表。
     */
    @Select("select " +
            "(select count(*) from follow where followee_id = #{uid}) as fans_count, " +
            "(select count(*) from follow where follower_id = #{uid}) as follow_count")
    UserFollowStatsVo userStats(@Param("uid") Long uid);
}
