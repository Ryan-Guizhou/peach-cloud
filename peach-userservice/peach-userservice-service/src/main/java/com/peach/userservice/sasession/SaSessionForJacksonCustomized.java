package com.peach.userservice.sasession;

import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/31 20:44
 */
@JsonIgnoreProperties({"timeout"})
public class SaSessionForJacksonCustomized extends SaSession implements Serializable{

    private static final long serialVersionUID = 1L;

    public SaSessionForJacksonCustomized() {
        super();
    }

    public SaSessionForJacksonCustomized(String id) {
        super(id);
    }

}
