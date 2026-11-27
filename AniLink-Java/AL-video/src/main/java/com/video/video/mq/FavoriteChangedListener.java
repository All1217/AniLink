package com.video.video.mq;

import com.video.common.constant.MqConstant;
import com.video.model.dto.social.FavoriteChangedDTO;
import com.video.video.service.FavoriteCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 收藏状态变更消费者（al-social → al-video）。
 *
 * <p>交换机 {@code social.favorite.topic}、路由键 {@code favorite.changed}。
 * 用 {@code eventId} 做 Redis SETNX 去重，保证重复投递只生效一次；
 * 落库失败时删除去重键并抛出，让 MQ 重投后可以再次处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoriteChangedListener {

    /** 去重键前缀，保留 24 小时覆盖消息重投窗口 */
    private static final String DEDUP_KEY_PREFIX = "social:favorite:changed:event:";
    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final FavoriteCollectService favoriteCollectService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.SOCIAL_FAVORITE_QUEUE, durable = "true"),
            exchange = @Exchange(name = MqConstant.SOCIAL_FAVORITE_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstant.FAVORITE_CHANGED_KEY
    ))
    public void listenFavoriteChanged(FavoriteChangedDTO event) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            // 事件契约缺失标识无法去重，直接丢弃避免重复计数
            log.warn("收到没有 eventId 的收藏事件，已丢弃：{}", event);
            return;
        }
        String dedupKey = DEDUP_KEY_PREFIX + event.getEventId();
        // 1.抢占去重键，抢不到说明该事件已消费过
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
        if (!Boolean.TRUE.equals(first)) {
            log.info("收藏事件重复投递，跳过：eventId={}", event.getEventId());
            return;
        }
        try {
            // 2.落库：user_video.collect + video_stats.collect
            favoriteCollectService.apply(event);
        } catch (Exception e) {
            // 3.失败时删除去重键，等待 MQ 重投后重新处理
            redisTemplate.delete(dedupKey);
            log.error("收藏事件消费失败，已释放去重键等待重投：eventId={}", event.getEventId(), e);
            throw e;
        }
    }
}
