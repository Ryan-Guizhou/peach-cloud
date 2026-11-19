package com.peach.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 可信 FileService 提交的白名单字段值与真实物理文件对象证据。
 *
 * <p>所有字段均由文件数据库生成；Auth 用同一角色绑定的字段策略重新投影，
 * 结果只能更少或脱敏，绝不返回未请求或未注册字段。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
public class SaasIamFileFieldReadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232002L;

    @Valid
    @NotNull
    private SaasIamFileObjectCheckDTO object;

    @NotNull
    private Map<String, Object> fields;
}
