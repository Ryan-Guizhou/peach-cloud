package com.peach.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 可信文件服务基于真实物理对象、当前动作和待更新字段构造的写入授权请求。
 * 所有写字段都需要与行级授权在同一绑定中明确被允许，不允许未知列。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
public class SaasIamFileFieldWriteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232004L;

    @Valid
    @NotNull
    private SaasIamFileObjectCheckDTO object;

    @NotEmpty
    private Map<String, String> changes;
}
