package com.video.social.mq;

import com.video.common.config.mq.RabbitMqHelper;
import com.video.common.constant.MqConstant;
import com.video.model.dto.social.FavoriteChangedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * 收藏状态变更事件发送器。
 *
 * <p>交换机 {@code social.favorite.topic}、路由键 {@code favorite.changed}，
 * 携带 {@code eventId} 供 al-video 幂等消费。事务内调用时延迟到提交后发送，避免回滚仍投递。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteEventPublisher {

    private final RabbitMqHelper rabbitMqHelper;

    /**
     * 收藏/取消收藏成功后发布事件。
     *
     * @param uid       用户 ID
     * @param vid       视频 ID
     * @param collected 变更后的收藏状态：1 已收藏，0 未收藏
     * @param delta     视频收藏计数增量：收藏 +1，取消收藏 -1
     */
    public void publishAfterCommit(Long uid, Long vid, int collected, int delta) {
        FavoriteChangedDTO event = new FavoriteChangedDTO();
        event.setEventId(UUID.randomUUID().toString());
        event.setUid(uid);
        event.setVid(vid);
        event.setCollected(collected);
        event.setDelta(delta);
        event.setOccurredAt(System.currentTimeMillis());
        // 事务内延迟到提交后再发，保证事件与落库结果一致
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    /** 实际投递：失败只记日志不回滚收藏结果，由对账兜底 */
    private void send(FavoriteChangedDTO event) {
        try {
            rabbitMqHelper.send(MqConstant.SOCIAL_FAVORITE_EXCHANGE, MqConstant.FAVORITE_CHANGED_KEY, event);
            log.info("收藏状态变更事件已投递：eventId={}, uid={}, vid={}, collected={}, delta={}",
                    event.getEventId(), event.getUid(), event.getVid(), event.getCollected(), event.getDelta());
        } catch (Exception e) {
            log.error("收藏状态变更事件投递失败：eventId={}, uid={}, vid={}",
                    event.getEventId(), event.getUid(), event.getVid(), e);
        }
    }
}
