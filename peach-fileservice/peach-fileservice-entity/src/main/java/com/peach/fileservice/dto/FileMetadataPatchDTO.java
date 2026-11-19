package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 文件元数据安全部分更新参数。
 *
 * <p>仅允许 displayName、bizTag、remark 等服务端固定白名单字段，
 * 每个字段由 Auth 中 FILE.EDIT 的同绑定字段 WRITE=ALLOW 独立授权。
 * 具体取值长度和 null 约束由 FileSaasAccessService 验证。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
@Schema(description = "文件受控元数据部分更新")
public class FileMetadataPatchDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232005L;

    @NotEmpty
    @Size(max = 3)
    @Schema(description = "仅可提交 displayName、bizTag、remark")
    private Map<String, String> changes;
}
