package com.peach.satoken.session;

import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * Sa-Token 会话定制实现。
 *
 * <p>仅用于 Jackson 序列化时忽略 `timeout` 属性，避免会话反序列化时携带不需要的字段。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@JsonIgnoreProperties({"timeout"})
public class PeachSaSessionForJacksonCustomized extends SaSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 无参构造。
     */
    public PeachSaSessionForJacksonCustomized() {
        super();
    }

    /**
     * 按会话 ID 创建对象。
     *
     * @param id 会话 ID
     */
    public PeachSaSessionForJacksonCustomized(String id) {
        super(id);
    }
}
