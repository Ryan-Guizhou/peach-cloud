package com.peach.common;

import java.io.Serial;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前上下文实体。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 */
@Data
public class CurrentContextEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1070217652749287153L;

    private String language;

    private CurrentUserDO currentUserDO;
}
