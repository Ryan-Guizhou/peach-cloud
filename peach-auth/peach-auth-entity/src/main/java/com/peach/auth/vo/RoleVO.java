package com.peach.userservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.userservice.entity.RoleDO;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:40
 */
@Data
public class RoleVO extends RoleDO implements Serializable {
    private static final long serialVersionUID = -3741124344646847872L;
}
