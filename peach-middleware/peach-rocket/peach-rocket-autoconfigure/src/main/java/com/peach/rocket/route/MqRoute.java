package com.peach.rocket.route;

/**
 * MQ路由。
 * <p>该模型表示一次消息发送在路由解析后的最终结果。发送端会根据这里的 topic、tag 和 key 继续构建
 * 标准消息信封，并调用 RocketMQ 原生 API 完成投递。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqRoute(String topic, String tag, String key) {
}
