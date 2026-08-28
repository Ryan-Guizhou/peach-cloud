package com.peach.fileservice.rest.internal;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.service.IFileDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 供内部调用方使用的文件辅助工具。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Validated
@Indexed
@RestController
@RequestMapping(FileApiConstant.INTERNAL_TOOLS_PREFIX)
@Tag(name = "文件内部工具", description = "供内部调用方使用的文件辅助工具")
@RequiredArgsConstructor
public class FileToolController {

        private final IFileDomainService fileDomainService;

    /**
     * 计算 multipart 文件 SHA-256 摘要。
     *
     * @param file 文件
     * @return 摘要和文件大小
     */
    @PostMapping("/sha256")
    @Operation(summary = "计算文件 SHA-256")
    public Response sha256(@RequestPart("file") MultipartFile file) {
        return Response.success(fileDomainService.calculateSha256(file));
    }
}
