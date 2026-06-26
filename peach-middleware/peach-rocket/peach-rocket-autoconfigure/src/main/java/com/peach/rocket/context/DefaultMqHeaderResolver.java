package com.peach.rocket.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认 MQ 消息头解析器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class DefaultMqHeaderResolver {

    /**
     * 解析业务传入的消息头。
     *
     * @param headers 业务消息头
     * @return 可写入消息信封的消息头
     */
    public Map<String, String> resolve(Map<String, String> headers) {
        return headers == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(headers);
    }
}
