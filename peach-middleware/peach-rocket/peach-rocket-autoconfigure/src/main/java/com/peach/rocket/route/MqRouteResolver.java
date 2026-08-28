package com.peach.rocket.route;

import com.peach.rocket.core.MqSendOptions;

/**
 * MQRoute解析器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqRouteResolver {

    /**
     * 解析消息发送路由。
     *
     * @param payload 业务消息
     * @param options 发送参数
     * @return 发送路由
     */
    MqRoute resolve(Object payload, MqSendOptions options);
}
