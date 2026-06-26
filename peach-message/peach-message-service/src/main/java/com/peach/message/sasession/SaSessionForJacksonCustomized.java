package com.peach.message.sasession;

import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26 17:53
 * @Description Jackson 序列化兼容的 SaSession
 */
@JsonIgnoreProperties({"timeout"})
public class SaSessionForJacksonCustomized extends SaSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public SaSessionForJacksonCustomized() {
        super();
    }

    public SaSessionForJacksonCustomized(String id) {
        super(id);
    }
}
