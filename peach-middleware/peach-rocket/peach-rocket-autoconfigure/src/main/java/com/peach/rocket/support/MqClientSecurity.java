package com.peach.rocket.support;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.util.StringUtils;

/**
 * 将统一身份配置应用于模板、独立 Consumer、事务 Producer 和 Admin。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
public final class MqClientSecurity {
    private MqClientSecurity() { }

    /** 在普通模板创建前同步属性；没有 Peach 覆盖时保留原生配置。 */
    public static void configureProperties(RocketMQProperties rocket, PeachRocketProperties peach) {
        if (rocket.getProducer() == null) {
            rocket.setProducer(new RocketMQProperties.Producer());
        }
        var producer = rocket.getProducer();
        if (StringUtils.hasText(peach.getClient().getAccessKey())) {
            if (StringUtils.hasText(producer.getAccessKey())
                    && (!peach.getClient().getAccessKey().equals(producer.getAccessKey())
                    || !peach.getClient().getSecretKey().equals(producer.getSecretKey()))) {
                throw new MqException("Conflicting RocketMQ credential configuration sources");
            }
            producer.setAccessKey(peach.getClient().getAccessKey());
            producer.setSecretKey(peach.getClient().getSecretKey());
        }
        if (peach.getClient().isTlsEnabled()) {
            producer.setTlsEnable(true);
        }
    }

    /** 返回只用于 SDK 签名的 Hook，不得打印或序列化该对象。 */
    public static RPCHook rpcHook(RocketMQProperties rocket, PeachRocketProperties peach) {
        configureProperties(rocket, peach);
        var producer = rocket.getProducer();
        boolean access = StringUtils.hasText(producer.getAccessKey());
        if (access != StringUtils.hasText(producer.getSecretKey())) {
            throw new MqException("Incomplete RocketMQ client credential pair");
        }
        return access ? new AclClientRPCHook(new SessionCredentials(producer.getAccessKey(), producer.getSecretKey())) : null;
    }

    /** namespace 使用原生 Broker namespace，区别于 Peach 环境命名前缀。 */
    public static void configureClient(ClientConfig client, RocketMQProperties rocket, PeachRocketProperties peach) {
        configureProperties(rocket, peach);
        client.setUseTLS(rocket.getProducer().isTlsEnable());
        client.setNamespace(rocket.getProducer().getNamespace());
        client.setNamespaceV2(rocket.getProducer().getNamespaceV2());
    }
}
