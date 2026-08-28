package com.peach.common;

import java.io.Serial;

import lombok.Data;

import java.io.Serializable;


/**
 * 当前用户上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 * @Description 当前用户上下文
 */
@Data
public class CurrentUserDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4682673142018982915L;

    private String userId;

    private String language;
}
