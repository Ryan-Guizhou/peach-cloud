package com.peach.common;

import lombok.Data;

import java.io.Serializable;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 * @Description 当前用户上下文
 */
@Data
public class CurrentUserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String language;
}
