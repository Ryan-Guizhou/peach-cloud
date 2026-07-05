package com.peach.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 */
@Data
public class CurrentContextEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String language;

    private CurrentUserDO currentUserDO;
}
