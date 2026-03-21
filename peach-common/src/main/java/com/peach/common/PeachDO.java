package com.peach.common;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description DO基类 - 包含所有实体类的公共审计字段
 *               包含：创建时间、创建者信息、更新时间、更新者信息
 */
@Data
public class PeachDO implements Serializable {

    @Column(name = "CREATE_TIME")
    private String createTime;     // 记录创建时间，格式：yyyy-MM-dd HH:mm:ss

    @Column(name = "CREATE_USER_ID")
    private String createUserId;   // 创建人ID，用于审计追踪

    @Column(name = "CREATE_USER_CODE")
    private String createUserCode; // 创建人账号

    @Column(name = "CREATE_USER_NAME")
    private String createUserName; // 创建人姓名

    @Column(name = "MODIFY_TIME")
    private String modifyTime;     // 记录最后修改时间

    @Column(name = "MODIFY_USER_ID")
    private String modifyUserId;  // 修改人ID

    @Column(name = "MODIFY_USER_CODE")
    private String modifyUserCode; // 修改人账号

    @Column(name = "UPDATE_USER_NAME")
    private String modifyUserName; // 修改人姓名
}