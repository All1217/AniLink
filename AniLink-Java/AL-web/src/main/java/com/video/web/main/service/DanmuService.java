package com.video.web.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.video.common.result.Result;
import com.video.model.entity.Danmu;
import com.video.web.main.vo.RecQueryVo;

import java.util.List;

/** 弹幕标签过滤（Phase 3 起弹幕读写归 al-danmaku，本接口只服务仍留在 AL-web 的标签接口）。 */
public interface DanmuService extends IService<Danmu> {
    Result<List<Danmu>> filterUserByTags(RecQueryVo queryVo);
}
