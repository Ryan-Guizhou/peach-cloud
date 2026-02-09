package com.peach.message.core.context;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:16
 */
@Data
@Builder
public class ContextDTO implements Serializable {

    private static final long serialVersionUID = 5680749760384651055L;

    private String userId;

    private String userToken;

    private String userIp;

    private String userHost;
}
