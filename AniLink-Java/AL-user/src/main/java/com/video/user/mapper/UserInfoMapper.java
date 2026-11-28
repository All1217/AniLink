package com.video.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.video.api.user.dto.AdminUserRefVo;
import com.video.model.entity.UserInfo;
import com.video.model.entity.UserStats;
import com.video.user.vo.UserInfoQueryVo;
import com.video.user.vo.UserVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
    @Update("update user set password = #{password}, password_updated_at = now() where uid = #{uid}")
    int updatePassword(@Param("uid") Long uid,
                       @Param("password") String password);

    /** 管理端：更新账号状态（0 正常 / 1 封禁） */
    @Update("update user set status = #{status} where uid = #{uid}")
    int updateStatus(@Param("uid") Long uid, @Param("status") Integer status);

    /** 管理端：更新账号角色（0 普通 / 1 管理员 / 2 超级管理员） */
    @Update("update user set role = #{role} where uid = #{uid}")
    int updateRole(@Param("uid") Long uid, @Param("role") Integer role);

    /** 管理端：按完整 URL 查用户头像/背景引用，对象存储删除前校验用 */
    @Select("select uid as id, nickname as name, 'user' as type, " +
            "case when avatar = #{url} then 'avatar' else 'background' end as field " +
            "from user where avatar = #{url} or background = #{url} limit 20")
    List<AdminUserRefVo> findUserRefs(@Param("url") String url);
}
