package com.video.web.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.web.admin.vo.AdminUserQueryVo;
import com.video.web.admin.vo.AdminUserVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 管理端用户查询与状态/角色更新。 */
public interface AdminUserMapper {

    /** 分页查询用户；响应字段不含密码。 */
    IPage<AdminUserVo> pageAdmin(IPage<AdminUserVo> page, @Param("query") AdminUserQueryVo query);

    /** 用户详情（不含密码）。 */
    AdminUserVo getAdminDetail(@Param("uid") Long uid);

    /** 更新账号状态（0 正常 / 1 封禁）。 */
    @Update("update user set status = #{status} where uid = #{uid}")
    int updateStatus(@Param("uid") Long uid, @Param("status") Integer status);

    /** 更新角色（0 普通 / 1 管理员 / 2 超级管理员）。 */
    @Update("update user set role = #{role} where uid = #{uid}")
    int updateRole(@Param("uid") Long uid, @Param("role") Integer role);
}
