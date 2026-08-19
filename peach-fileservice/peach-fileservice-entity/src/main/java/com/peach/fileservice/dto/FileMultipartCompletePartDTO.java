package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 分片上传完成信息数据传输对象
 *
 * <p>用于分片上传场景的完成阶段，封装单个分片的元数据信息，
 * 包含分片序号（partNumber）和分片ETag标识，供服务端校验并
 * 合并各分片时作为请求参数传递。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>分片序号必须大于0，保证与上传顺序一致</li>
 *   <li>ETag不能为空，用于服务端校验分片完整性</li>
 *   <li>实现Serializable接口，支持序列化传输</li>
 * </ul>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@Schema(description = "分片完成信息")
public class FileMultipartCompletePartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1L, message = "partNumber必须大于0")
    @Schema(description = "分片序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer partNumber;

    @NotBlank(message = "eTag不能为空")
    @Schema(description = "分片ETag", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eTag;
}
