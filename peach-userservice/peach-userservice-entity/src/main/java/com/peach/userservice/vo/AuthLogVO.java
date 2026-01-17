package com.peach.userservice.vo;

import com.peach.userservice.entity.AuthLogDO;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
public class AuthLogVO extends AuthLogDO implements Serializable {
    private static final long serialVersionUID = 1L;
}
