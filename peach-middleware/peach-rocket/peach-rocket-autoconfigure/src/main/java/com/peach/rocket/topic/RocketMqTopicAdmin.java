package com.peach.rocket.topic;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.annotation.MqTransaction;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.support.RocketMqNaming;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.constant.PermName;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * RocketMQ Topic 自动创建器。
 *
 * <p>该组件在 Spring 容器单例 Bean 初始化完成后自动执行，扫描应用中所有需要使用的 Topic，
 * 并通过 RocketMQ Admin API 在 Broker 集群上自动创建这些 Topic。
 *
 * <p><b>Topic 来源：</b>
 * <ul>
 *   <li>显式配置：通过 {@code peach.rocket.topic.topics} 配置项指定</li>
 *   <li>消费者注解：扫描所有 {@code @MqConsumer} 注解中声明的 Topic</li>
 *   <li>事务注解：扫描所有 {@code @MqTransaction} 注解中声明的 Topic</li>
 * </ul>
 *
 * <p><b>执行时机：</b>
 * 实现 {@link SmartInitializingSingleton} 接口，在所有单例 Bean 实例化完成后执行，
 * 确保所有注解已被扫描并注册到 Spring 容器中。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class RocketMqTopicAdmin implements SmartInitializingSingleton {

    /**
     * RocketMQ 自动配置属性，用于获取 NameServer 地址等基本信息。
     */
    private final RocketMQProperties rocketMQProperties;

    /**
     * Peach RocketMQ 自定义配置属性，包含 Topic 自动创建相关配置。
     */
    private final PeachRocketProperties properties;

    /**
     * Spring 应用上下文，用于扫描带有特定注解的 Bean。
     */
    private final ApplicationContext applicationContext;

    public RocketMqTopicAdmin(RocketMQProperties rocketMQProperties,
                              PeachRocketProperties properties,
                              ApplicationContext applicationContext) {
        this.rocketMQProperties = rocketMQProperties;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * 在所有单例 Bean 实例化完成后执行 Topic 自动创建。
     *
     * <p>执行流程：
     * <ol>
     *   <li>检查是否开启自动创建（{@code peach.rocket.topic.auto-create}）</li>
     *   <li>收集所有需要创建的 Topic 名称</li>
     *   <li>初始化 RocketMQ Admin 客户端，连接 NameServer</li>
     *   <li>获取集群 Broker 信息，在每个 Broker 上创建或更新 Topic</li>
     * </ol>
     */
    @Override
    public void afterSingletonsInstantiated() {
        // 检查是否开启了 Topic 自动创建
        if (!properties.getTopic().isAutoCreate()) {
            return;
        }

        // 收集所有需要创建的 Topic
        Set<String> topics = collectTopics();
        if (topics.isEmpty()) {
            log.info("[mq-topic] no topic found for auto creation");
            return;
        }

        // 创建 RocketMQ Admin 工具实例，用于执行管理操作
        DefaultMQAdminExt admin = new DefaultMQAdminExt();
        admin.setNamesrvAddr(rocketMQProperties.getNameServer());
        admin.setInstanceName("peach-rocket-topic-admin-" + System.currentTimeMillis());

        try {
            // 启动 Admin 客户端
            admin.start();

            // 获取集群信息，包含所有 Broker 的地址
            ClusterInfo clusterInfo = admin.examineBrokerClusterInfo();

            // 在每个 Broker 上创建所有收集到的 Topic
            for (String topic : topics) {
                createTopicOnAllBrokers(admin, clusterInfo, topic);
            }

        } catch (Exception ex) {
            throw new MqException("Failed to auto create RocketMQ topic", ex);
        } finally {
            // 释放 Admin 客户端资源
            admin.shutdown();
        }
    }

    /**
     * 收集所有需要自动创建的 Topic 名称。
     *
     * <p>Topic 来源（按优先级排序）：
     * <ol>
     *   <li>显式配置：{@code peach.rocket.topic.topics}</li>
     *   <li>消费者注解：{@code @MqConsumer} 中声明的 Topic</li>
     *   <li>事务注解：{@code @MqTransaction} 中声明的 Topic</li>
     * </ol>
     *
     * <p>所有 Topic 名称都会经过 {@link RocketMqNaming#normalizeTopic} 标准化处理，
     * 统一添加环境前缀（如 {@code dev-}、{@code prod-}）进行环境隔离。
     *
     * <p>使用 {@link LinkedHashSet} 保持插入顺序并自动去重。
     *
     * @return 去重后的 Topic 名称集合
     */
    private Set<String> collectTopics() {
        // 使用 LinkedHashSet 保持顺序并自动去重
        Set<String> topics = new LinkedHashSet<String>();

        // 1. 从配置文件中的显式配置收集 Topic
        for (String topic : properties.getTopic().getTopics()) {
            topics.add(RocketMqNaming.normalizeTopic(topic, properties));
        }

        // 2. 从 @MqConsumer 注解中收集 Topic
        if (properties.getTopic().isIncludeConsumerTopics()) {
            // 获取所有标注了 @MqConsumer 的 Bean
            for (Object bean : applicationContext.getBeansWithAnnotation(MqConsumer.class).values()) {
                // 绕过 AOP 代理获取目标类上的注解
                MqConsumer consumer = AnnotationUtils.findAnnotation(
                        AopUtils.getTargetClass(bean), MqConsumer.class);
                if (consumer != null) {
                    topics.add(RocketMqNaming.normalizeTopic(consumer.topic(), properties));
                }
            }
        }

        // 3. 从 @MqTransaction 注解中收集 Topic
        if (properties.getTopic().isIncludeTransactionTopics()) {
            // 获取所有标注了 @MqTransaction 的 Bean
            for (Object bean : applicationContext.getBeansWithAnnotation(MqTransaction.class).values()) {
                // 绕过 AOP 代理获取目标类上的注解
                MqTransaction transaction = AnnotationUtils.findAnnotation(
                        AopUtils.getTargetClass(bean), MqTransaction.class);
                if (transaction != null) {
                    topics.add(RocketMqNaming.normalizeTopic(transaction.topic(), properties));
                }
            }
        }

        return topics;
    }

    /**
     * 在集群的所有 Broker 上创建或更新指定的 Topic。
     *
     * <p>操作说明：
     * <ul>
     *   <li>使用 {@link TopicConfig} 封装 Topic 配置信息</li>
     *   <li>读取队列数配置：{@code peach.rocket.topic.readQueueNums} 和
     *       {@code peach.rocket.topic.writeQueueNums}</li>
     *   <li>权限设置为读写权限（{@link PermName#PERM_READ} | {@link PermName#PERM_WRITE}）</li>
     *   <li>遍历集群中所有 Broker 的 Master 和 Slave 节点，分别执行创建操作</li>
     * </ul>
     *
     * <p><b>注意：</b>RocketMQ 的 {@code createAndUpdateTopicConfig} 是幂等操作，
     * 如果 Topic 已存在，会更新其配置；如果不存在，则创建新 Topic。
     *
     * @param admin        RocketMQ Admin 客户端实例
     * @param clusterInfo  集群信息，包含所有 Broker 的地址映射
     * @param topic        需要创建的 Topic 名称
     * @throws Exception 创建失败时抛出异常
     */
    private void createTopicOnAllBrokers(DefaultMQAdminExt admin,
                                         ClusterInfo clusterInfo,
                                         String topic) throws Exception {
        // 构建 Topic 配置对象
        TopicConfig topicConfig = new TopicConfig(topic);
        topicConfig.setReadQueueNums(properties.getTopic().getReadQueueNums());
        topicConfig.setWriteQueueNums(properties.getTopic().getWriteQueueNums());
        topicConfig.setPerm(PermName.PERM_READ | PermName.PERM_WRITE);

        // 遍历所有 Broker
        for (Map.Entry<String, BrokerData> entry : clusterInfo.getBrokerAddrTable().entrySet()) {
            String brokerName = entry.getKey();
            BrokerData brokerData = entry.getValue();

            // 遍历该 Broker 的所有地址（包括 Master 和 Slave）
            for (String brokerAddr : brokerData.getBrokerAddrs().values()) {
                // 执行 Topic 创建或更新（幂等操作）
                admin.createAndUpdateTopicConfig(brokerAddr, topicConfig);

                log.info("[mq-topic] topic created or updated. topic={} brokerName={} brokerAddr={} readQueueNums={} writeQueueNums={}",
                        topic, brokerName, brokerAddr,
                        topicConfig.getReadQueueNums(), topicConfig.getWriteQueueNums());
            }
        }
    }
}