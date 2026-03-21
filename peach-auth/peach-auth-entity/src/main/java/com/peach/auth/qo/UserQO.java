package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 17:13
 */
@Data
@Schema(description = "用户查询参数")
public class UserQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 3457123480263246325L;

    private String id;

    private String username;

    private String phone;

    private List<String> idList;
}
