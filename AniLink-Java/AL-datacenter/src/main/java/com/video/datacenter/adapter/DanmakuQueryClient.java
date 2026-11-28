package com.video.datacenter.adapter;

import com.video.common.result.Result;
import com.video.model.entity.Danmu;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 弹幕域读取适配：弹幕明细归 al-danmaku，数据中心只读其公开列表接口做标签聚合。
 *
 * <p>不新增 AL-danmaku 内部契约：公开列表已带 status=1 过滤，形状与旧弹幕标签 SQL 一致，
 * 跨域拼接（标签 × 弹幕）在数据中心内存完成，避免跨域 JOIN。</p>
 */
@FeignClient(name = "danmakuservice", contextId = "danmakuQueryClient",
        url = "${al.danmaku.service-url:}", path = "/main/danmu")
public interface DanmakuQueryClient {

    /** 取某视频的全部公开弹幕（status=1） */
    @GetMapping("/public/getDanmuList")
    Result<List<Danmu>> listByVid(@RequestParam("vid") Long vid);
}
