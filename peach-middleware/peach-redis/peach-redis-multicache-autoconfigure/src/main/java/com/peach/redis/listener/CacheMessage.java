
package com.peach.redis.listener;



import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 16:09
 * @Description 多节点缓存通知消息
 */
@Data
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = -6221995438342888610L;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存key
     */
    private Object key;

    /**
     * 消息发起者
     */
    private Integer sender;

    public CacheMessage() {
    }


    public CacheMessage(String cacheName, Object key, Integer sender) {
        this.cacheName = cacheName;
        this.key = key;
        this.sender = sender;
    }

}
