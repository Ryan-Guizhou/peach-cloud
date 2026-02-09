package com.peach.userservice.qo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 17:13
 */
@Data
public class UserQO implements Serializable {

    private static final long serialVersionUID = 3457123480263246325L;

    private String id;

    private String username;

    private String phone;

    private List<String> idList;
}
