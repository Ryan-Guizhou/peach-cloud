package com.peach.redis.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.stream.StreamListener;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:46
 */
@Slf4j
public class RedisStreamLinstener implements StreamListener<String, ObjectRecord<String, String>> {

    private final MessageConsumer messageConsumer;

    public RedisStreamLinstener(MessageConsumer messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try{
            RecordId messageId = message.getId();
            String value = message.getValue();
            log.info("redis stream consumption has reached the data messageId : {}, streamName : {}, message : {}",
                    messageId, message.getStream(), value);
            messageConsumer.accept(message);
        }catch (Exception e){
            log.error("onMessage error",e);
        }
    }
}
