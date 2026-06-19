package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 分片上传完成数据传输对象
 *
 * <p>用于文件分片上传完成后的合并确认，包含上传会话ID及各分片的完成信息
 * （分片序号与ETag），服务端据此校验并合并所有已上传的分片，最终生成完整文件。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "分片上传完成参数")
public class FileMultipartCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "sessionId不能为空")
    @Schema(description = "上传会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionId;

    @Valid
    @NotEmpty(message = "parts不能为空")
    @Schema(description = "分片完成列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FileMultipartCompletePartDTO> parts;
}
