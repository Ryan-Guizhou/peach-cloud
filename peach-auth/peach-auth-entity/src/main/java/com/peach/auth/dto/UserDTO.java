package com.peach.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 */
@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 5268266371854905203L;

    private String id;

    private String username;
}
