package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 有界批量文件操作请求，不能使用客户端自报的租户或机构范围。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
@Schema(description = "受控批量文件操作")
public class FileBatchActionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232006L;

    @NotEmpty
    @Size(max = 50)
    @Schema(description = "最多 50 个互不重复的永久文件业务编号")
    private List<@NotBlank String> fileIds;
}
