package com.video.social.mq;

import com.video.common.config.mq.RabbitMqHelper;
import com.video.common.constant.MqConstant;
import com.video.model.dto.social.FavoriteChangedDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/** 收藏事件：交换机/路由键固定，消息体字段完整，发送失败不抛出。 */
@ExtendWith(MockitoExtension.class)
class FavoriteEventPublisherTest {

    @Mock
    private RabbitMqHelper rabbitMqHelper;

    @Test
    void publishesFavoriteChangedEvent() {
        FavoriteEventPublisher publisher = new FavoriteEventPublisher(rabbitMqHelper);

        publisher.publishAfterCommit(7L, 35L, 1, 1);

        ArgumentCaptor<FavoriteChangedDTO> captor = ArgumentCaptor.forClass(FavoriteChangedDTO.class);
        verify(rabbitMqHelper).send(eq(MqConstant.SOCIAL_FAVORITE_EXCHANGE), eq(MqConstant.FAVORITE_CHANGED_KEY), captor.capture());
        FavoriteChangedDTO event = captor.getValue();
        assertNotNull(event.getEventId());
        assertEquals(7L, event.getUid());
        assertEquals(35L, event.getVid());
        assertEquals(1, event.getCollected());
        assertEquals(1, event.getDelta());
        assertTrue(event.getOccurredAt() > 0);
    }

    @Test
    void sendFailureIsSwallowed() {
        FavoriteEventPublisher publisher = new FavoriteEventPublisher(rabbitMqHelper);
        // 发送异常不影响收藏主流程，只记录日志
        doThrow(new RuntimeException("mq down")).when(rabbitMqHelper).send(anyString(), anyString(), any());

        assertDoesNotThrow(() -> publisher.publishAfterCommit(7L, 35L, 0, -1));
    }
}
