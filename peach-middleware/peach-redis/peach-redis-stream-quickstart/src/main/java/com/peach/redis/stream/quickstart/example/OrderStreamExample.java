package com.peach.redis.stream.quickstart.example;

import com.peach.redis.stream.RedisStreamPushHandler;
import com.peach.redis.stream.quickstart.consumer.OrderStatusMessageConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Stream 能力样例：push 获取 RecordId、消费确认、同 RecordId 幂等去重。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class OrderStreamExample {

    private final RedisStreamPushHandler redisStreamPushHandler;
    private final OrderStatusMessageConsumer messageConsumer;

    /**
     * 推送订单状态并等待消费组消费成功。
     */
    public String pushAndConsume(String orderId, String status) {
        String payload = "orderId=" + orderId + ",status=" + status;
        messageConsumer.resetForExpect(1, orderId);
        RecordId recordId = redisStreamPushHandler.push(payload);
        boolean consumed;
        try {
            consumed = messageConsumer.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("await consume interrupted", ex);
        }
        if (!consumed || messageConsumer.getLastMessage() == null
                || !messageConsumer.getLastMessage().contains(orderId)) {
            throw new IllegalStateException("consume failed, recordId=" + recordId);
        }
        log.info("pushAndConsume ok, recordId={}, payload={}", recordId, messageConsumer.getLastMessage());
        return messageConsumer.getLastMessage();
    }

    /**
     * 首次正常消费后，对同一 RecordId 回放，验证业务侧幂等（accepted 不增加 / duplicate=1）。
     */
    public void pushThenReplayDuplicate(String orderId, String status) {
        String payload = "orderId=" + orderId + ",status=" + status;
        messageConsumer.resetForExpect(1, orderId);
        RecordId recordId = redisStreamPushHandler.push(payload);
        boolean consumed;
        try {
            consumed = messageConsumer.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("await consume interrupted", ex);
        }
        if (!consumed || recordId == null || messageConsumer.getLastMessage() == null
                || !messageConsumer.getLastMessage().contains(orderId)) {
            throw new IllegalStateException("first consume failed");
        }
        int acceptedAfterFirst = messageConsumer.getAcceptedCount();

        ObjectRecord<String, String> replay = StreamRecords.newRecord()
                .in("order-status")
                .ofObject(payload)
                .withId(recordId);
        messageConsumer.replay(replay);

        if (messageConsumer.getAcceptedCount() != acceptedAfterFirst || messageConsumer.getDuplicateCount() != 1) {
            throw new IllegalStateException("idempotent check failed, accepted="
                    + messageConsumer.getAcceptedCount() + ", duplicates=" + messageConsumer.getDuplicateCount());
        }
        log.info("idempotent ok, recordId={}, accepted={}, duplicates={}",
                recordId, messageConsumer.getAcceptedCount(), messageConsumer.getDuplicateCount());
    }
}
