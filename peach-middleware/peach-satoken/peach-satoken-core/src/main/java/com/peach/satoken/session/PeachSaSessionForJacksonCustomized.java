package com.peach.satoken.session;

import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties({"timeout"})
public class PeachSaSessionForJacksonCustomized extends SaSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public PeachSaSessionForJacksonCustomized() {
        super();
    }

    public PeachSaSessionForJacksonCustomized(String id) {
        super(id);
    }
}
