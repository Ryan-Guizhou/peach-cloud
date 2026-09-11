package com.peach.redis.stream.quickstart.consumer;

import com.peach.redis.stream.MessageConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 订单状态流消费者：实现 {@link MessageConsumer}，支持等待消费完成与 RecordId 幂等去重。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Component
public class OrderStatusMessageConsumer implements MessageConsumer {

    private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(0));
    private final AtomicReference<String> expectContains = new AtomicReference<>();
    private final AtomicReference<String> lastMessage = new AtomicReference<>();
    private final AtomicReference<String> lastRecordId = new AtomicReference<>();
    private final AtomicInteger acceptedCount = new AtomicInteger();
    private final AtomicInteger duplicateCount = new AtomicInteger();
    private final Set<String> seenRecordIds = ConcurrentHashMap.newKeySet();

    @Override
    public void accept(ObjectRecord<String, String> message) {
        String recordId = message.getId() == null ? null : message.getId().getValue();
        String payload = message.getValue();
        String filter = expectContains.get();
        if (filter != null && (payload == null || !payload.contains(filter))) {
            log.info("stream ignored by filter, recordId={}", recordId);
            return;
        }
        if (recordId != null && !seenRecordIds.add(recordId)) {
            duplicateCount.incrementAndGet();
            log.info("stream duplicate skipped, recordId={}", recordId);
            return;
        }
        lastRecordId.set(recordId);
        lastMessage.set(payload);
        acceptedCount.incrementAndGet();
        CountDownLatch latch = latchRef.get();
        if (latch != null) {
            latch.countDown();
        }
        log.info("stream consumed, recordId={}, payload={}", recordId, payload);
    }

    /**
     * 重置统计，仅接受 payload 包含指定片段的消息。
     */
    public void resetForExpect(int expected, String payloadContains) {
        lastMessage.set(null);
        lastRecordId.set(null);
        acceptedCount.set(0);
        duplicateCount.set(0);
        seenRecordIds.clear();
        expectContains.set(payloadContains);
        latchRef.set(new CountDownLatch(Math.max(expected, 0)));
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = latchRef.get();
        return latch != null && latch.await(timeout, unit);
    }

    public String getLastMessage() {
        return lastMessage.get();
    }

    public String getLastRecordId() {
        return lastRecordId.get();
    }

    public int getAcceptedCount() {
        return acceptedCount.get();
    }

    public int getDuplicateCount() {
        return duplicateCount.get();
    }

    /**
     * 模拟重复投递：对同一 RecordId 再次调用 accept。
     */
    public void replay(ObjectRecord<String, String> message) {
        accept(message);
    }
}
