package com.peach.fileservice.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.dto.FileMultipartCompleteDTO;
import com.peach.fileservice.dto.FileMultipartInitDTO;
import com.peach.fileservice.dto.FileMultipartPartUrlDTO;
import com.peach.fileservice.service.IFileDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 文件分片上传初始化、分片 URL、完成与中止接口。
 * <p>该控制器只承载分片上传会话相关操作，路径仍保持在 {@code /file/internal/multipart/**}，
 * 便于与普通文件资源管理接口区分。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12
 */
@Indexed
@Validated
@RestController
@RequestMapping(FileApiConstant.INTERNAL_PREFIX + "/multipart")
@Tag(name = "文件分片上传接口", description = "文件分片上传初始化、分片 URL、完成与中止接口")
@RequiredArgsConstructor
public class FileMultipartController {

        private final IFileDomainService fileDomainService;

    /**
     * 初始化分片上传。
     *
     * @param data 分片上传初始化参数
     * @return 分片上传初始化结果
     */
    @PostMapping("/init")
    @Operation(summary = "初始化分片上传")
    public Response initMultipart(@Valid @RequestBody FileMultipartInitDTO data) {
        return Response.success(fileDomainService.initMultipartUpload(data));
    }

    /**
     * 获取指定分片的预签名上传地址。
     *
     * @param data 分片 URL 参数
     * @return 分片上传地址
     */
    @PostMapping("/part-url")
    @Operation(summary = "获取分片上传地址")
    public Response partUrl(@Valid @RequestBody FileMultipartPartUrlDTO data) {
        return Response.success(fileDomainService.prepareMultipartPart(data));
    }

    /**
     * 完成分片上传。
     *
     * @param data 分片完成参数
     * @return 文件上传结果
     */
    @PostMapping("/complete")
    @Operation(summary = "完成分片上传")
    public Response completeMultipart(@Valid @RequestBody FileMultipartCompleteDTO data) {
        return Response.success(fileDomainService.completeMultipartUpload(data));
    }

    /**
     * 中止分片上传。
     *
     * @param sessionId 上传会话 ID
     * @return 统一响应
     */
    @PostMapping("/abort/{sessionId}")
    @Operation(summary = "中止分片上传")
    public Response abortMultipart(@NotBlank(message = "sessionId不能为空") @PathVariable String sessionId) {
        fileDomainService.abortMultipartUpload(sessionId);
        return Response.success();
    }
}
