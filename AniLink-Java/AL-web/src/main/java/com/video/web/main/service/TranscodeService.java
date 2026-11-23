package com.video.web.main.service;

/**
 * 视频 HLS 转码服务。
 * 投稿/启动补跑只负责入队，真正的转码在单线程池里异步执行，期间播放器继续用原 MP4。
 */
public interface TranscodeService {

    /** 把单个稿件加入转码队列（幂等调用无副作用，状态由数据库守卫） */
    void submit(Long vid);

    /** 启动时补跑：重置中断的「转码中」，再把所有待转码稿件入队 */
    void submitPending();
}
