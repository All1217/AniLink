package com.video.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.vo.UserInfoQueryVo;
import com.video.user.vo.UserVo;
import org.apache.ibatis.annotations.Select;

public interface UserInfoMapper extends BaseMapper<UserInfo> {
    UserInfo selectOneByUsername(String username);

    @Select("select * from user where uid = #{userId}")
    UserInfo getById(Long userId);

    @Select("select * from user_stats where uid = #{uid}")
    UserStats getUserStatsByUid(Long uid);

    void updateUserStats(UserStats userStats);

    void updateUserInfo(UserInfo userInfo);
    void inserUserStats(UserStats query);

    UserVo getUserVoByUid(UserInfoQueryVo queryVo);

    /** 更新密码哈希（登录时把历史 MD5 升级为 bcrypt，或用户改密），同时记录修改时间 */
    @org.apache.ibatis.annotations.Update("update user set password = #{password}, password_updated_at = now() where uid = #{uid}")
    int updatePassword(@org.apache.ibatis.annotations.Param("uid") Long uid,
                       @org.apache.ibatis.annotations.Param("password") String password);
}
