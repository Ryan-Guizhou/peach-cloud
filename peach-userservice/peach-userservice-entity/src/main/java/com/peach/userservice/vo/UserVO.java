package com.peach.userservice.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = -3741124344646847872L;

    private String id;

    private String username;

    private String email;

    private String phone;

    private String address;

    private String country;
}
