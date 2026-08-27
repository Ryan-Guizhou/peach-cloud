package com.peach.rocket.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqConsumeMode;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.core.MqMessageModel;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.support.RocketMqNaming;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * 基于 {@link MqConsumer} 的 RocketMQ 动态消费者注册器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class DynamicRocketMqConsumerRegistrar implements SmartLifecycle, ApplicationContextAware {

    private final RocketMQProperties rocketMQProperties;
    private final PeachRocketProperties properties;
    private final MqConsumerInvoker invoker;
    private final List<DefaultMQPushConsumer> consumers = new ArrayList<DefaultMQPushConsumer>();
    private ApplicationContext applicationContext;
    private volatile boolean running;

    public DynamicRocketMqConsumerRegistrar(RocketMQProperties rocketMQProperties, PeachRocketProperties properties, MqConsumerInvoker invoker) {
        this.rocketMQProperties = rocketMQProperties;
        this.properties = properties;
        this.invoker = invoker;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        if (running || !properties.getConsumer().isDynamicRegister()) {
            return;
        }
        Map<String, MqMessageHandler> handlers = applicationContext.getBeansOfType(MqMessageHandler.class, false, false);
        log.info("[mq-consumer] scan dynamic consumers. handlerCount={} dynamicRegister={}", handlers.size(), properties.getConsumer().isDynamicRegister());
        for (Map.Entry<String, MqMessageHandler> entry : handlers.entrySet()) {
            register(entry.getKey(), entry.getValue());
        }
        running = true;
        log.info("[mq-consumer] dynamic consumers started. count={}", consumers.size());
    }

    @Override
    public void stop() {
        for (DefaultMQPushConsumer consumer : consumers) {
            consumer.shutdown();
        }
        consumers.clear();
        running = false;
        log.info("[mq-consumer] dynamic consumers stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 50;
    }


    /**
     * 动态注册 RocketMQ 消费者
     *
     * 该方法在运行时动态创建并启动一个 RocketMQ 消费者实例，
     * 用于消费被 @MqConsumer 注解标记的 Handler 所指定的 Topic 消息。
     *
     * @param beanName Spring 容器中该 Bean 的名称，用于日志标识和实例命名
     * @param handler 实现了 MqMessageHandler 接口的 Bean，包含实际的消息处理逻辑
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void register(final String beanName, final MqMessageHandler handler) {
        Class<?> targetClass = AopUtils.getTargetClass(handler);
        final MqConsumer annotation = AnnotationUtils.findAnnotation(targetClass, MqConsumer.class);
        if (annotation == null) {
            return;
        }
        final String topic = RocketMqNaming.normalizeTopic(annotation.topic(), properties);
        final String consumerGroup = RocketMqNaming.normalizeConsumerGroup(annotation.consumerGroup());
        String tagExpression = StringUtils.hasText(annotation.tag()) ? annotation.tag() : "*";
        try {
            final DefaultMQPushConsumer consumer = createConsumer(beanName, annotation, topic, consumerGroup, tagExpression);
            registerMessageListener(consumer, beanName, consumerGroup, handler, annotation);
            consumer.start();
            consumers.add(consumer);
            log.info("[mq-consumer] dynamic consumer registered. bean={} topic={} tag={} group={} mode={} model={}",
                    beanName, topic, tagExpression, consumerGroup, annotation.consumeMode(), annotation.messageModel());
        } catch (Exception ex) {
            throw new MqException("Failed to register RocketMQ dynamic consumer for bean " + beanName, ex);
        }
    }

    private DefaultMQPushConsumer createConsumer(String beanName, MqConsumer annotation,
                                                   String topic, String consumerGroup, String tagExpression) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
        consumer.setInstanceName(properties.getAppName() + "-" + beanName + "-" + System.nanoTime());
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setConsumeThreadMin(properties.getConsumer().getConsumeThreadMin());
        consumer.setConsumeThreadMax(properties.getConsumer().getConsumeThreadMax());
        if (annotation.maxReconsumeTimes() >= 0) {
            consumer.setMaxReconsumeTimes(annotation.maxReconsumeTimes());
        }
        consumer.setMessageModel(annotation.messageModel() == MqMessageModel.BROADCASTING
                ? MessageModel.BROADCASTING : MessageModel.CLUSTERING);
        consumer.subscribe(topic, tagExpression);
        return consumer;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerMessageListener(DefaultMQPushConsumer consumer, String beanName, String consumerGroup,
                                         MqMessageHandler handler, MqConsumer annotation) {
        if (annotation.consumeMode() == MqConsumeMode.ORDERLY) {
            consumer.registerMessageListener((MessageListenerOrderly) (messages, context) ->
                    consumeOrderly(beanName, consumerGroup, handler, annotation, messages));
        } else {
            consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) ->
                    consumeConcurrently(beanName, consumerGroup, handler, annotation, messages));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ConsumeOrderlyStatus consumeOrderly(String beanName, String consumerGroup, MqMessageHandler handler,
                                                  MqConsumer annotation, List<MessageExt> messages) {
        try {
            dispatchMessages(beanName, consumerGroup, handler, annotation, messages);
            return ConsumeOrderlyStatus.SUCCESS;
        } catch (RuntimeException ex) {
            log.error("[mq-consumer-error] consumer={} group={} mode=orderly exception={}", beanName, consumerGroup,
                    ex.getClass().getName(), ex);
            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ConsumeConcurrentlyStatus consumeConcurrently(String beanName, String consumerGroup, MqMessageHandler handler,
                                                            MqConsumer annotation, List<MessageExt> messages) {
        try {
            dispatchMessages(beanName, consumerGroup, handler, annotation, messages);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (RuntimeException ex) {
            log.error("[mq-consumer-error] consumer={} group={} mode=concurrently exception={}", beanName, consumerGroup,
                    ex.getClass().getName(), ex);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dispatchMessages(String beanName, String consumerGroup, MqMessageHandler handler, MqConsumer annotation,
                                  List<MessageExt> messages) {
        for (MessageExt message : messages) {
            logReceive(beanName, consumerGroup, message);
            invoker.invoke(handler, beanName, message.getBody(), annotation, message.getMsgId(), message.getReconsumeTimes());
        }
    }

    /**
     * 记录消息接收日志
     *
     * 该方法用于在消费者接收到消息时，打印结构化的日志信息，
     * 便于后续的问题排查、监控告警和消息链路追踪。
     *
     * 日志中包含的关键信息：
     * <ul>
     *   <li>消费者标识：beanName + consumerGroup，用于定位是哪个消费者接收了消息</li>
     *   <li>消息元数据：Topic、Tag、QueueId、QueueOffset，用于定位消息在 Broker 中的位置</li>
     *   <li>消息标识：messageId（全局唯一），用于去重和链路追踪</li>
     *   <li>消费状态：reconsumeTimes，用于判断消息是否被重试过</li>
     *   <li>消息大小：bodySize，用于监控大消息</li>
     * </ul>
     *
     * @param beanName       Spring 容器中消费者 Bean 的名称，用于标识具体是哪个 Handler 实例
     * @param consumerGroup  消费者组名称，用于标识消费组
     * @param message        RocketMQ 消息对象（MessageExt），包含消息的所有元数据和消息体
     */
    private void logReceive(String beanName, String consumerGroup, MessageExt message) {
        // 从消息属性中获取 Tag（注意：MessageExt.getTags() 有时可能为空，从属性中获取更可靠）
        String tag = message.getProperty(MessageConst.PROPERTY_TAGS);

        // 打印结构化的消息接收日志
        // 包含：消费者标识、消息元数据、消息体大小等信息
        log.info("[mq-consumer-receive] consumer={} group={} topic={} tag={} queueId={} queueOffset={} messageId={} reconsumeTimes={} bodySize={}",
                beanName,                                           // 消费者 Bean 名称
                consumerGroup,                                      // 消费者组
                message.getTopic(),                                 // 消息所属 Topic
                tag,                                                // 消息 Tag（子主题）
                message.getQueueId(),                               // 消息所在的队列 ID（用于顺序消费）
                message.getQueueOffset(),                           // 消息在队列中的偏移量（位置）
                message.getMsgId(),                                 // 消息全局唯一 ID（由 Broker 生成）
                message.getReconsumeTimes(),                        // 消息已重试次数（0 表示首次消费）
                message.getBody() == null ? 0 : message.getBody().length  // 消息体字节大小
        );
    }
}
