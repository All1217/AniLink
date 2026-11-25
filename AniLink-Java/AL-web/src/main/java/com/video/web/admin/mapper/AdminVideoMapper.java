package com.video.web.admin.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.video.web.admin.vo.AdminVideoQueryVo;
import com.video.web.admin.vo.AdminVideoVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/** 管理端稿件查询与状态更新。 */
public interface AdminVideoMapper {

    /** 分页查询稿件；不传 status 时默认排除已删除。 */
    IPage<AdminVideoVo> pageAdmin(IPage<AdminVideoVo> page, @Param("query") AdminVideoQueryVo query);

    /** 稿件详情（含作者昵称）。 */
    AdminVideoVo getAdminDetail(@Param("vid") Long vid);

    /** 更新审核/删除状态；下架时写入 delete_date。 */
    @Update("update video set status = #{status}, delete_date = #{deleteDate} where vid = #{vid}")
    int updateStatus(@Param("vid") Long vid, @Param("status") Integer status,
                     @Param("deleteDate") Date deleteDate);

    /** 更新公开/仅自己可见。 */
    @Update("update video set visible = #{visible} where vid = #{vid}")
    int updateVisible(@Param("vid") Long vid, @Param("visible") Integer visible);
}
