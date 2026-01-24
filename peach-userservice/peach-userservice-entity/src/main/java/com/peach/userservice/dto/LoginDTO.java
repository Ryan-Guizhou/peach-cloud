package com.peach.userservice.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:36
 */
@Data
public class LoginDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;

    private String password;
}
