package com.peach.fileservice.rest.external;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.dto.FileExternalUploadDTO;
import com.peach.fileservice.service.IFileDomainService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 供其他业务服务调用的文件 API。
 * <p>外部接口只暴露业务文件 ID 和临时 URL，不暴露 bucket、objectKey 或本地路径。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Validated
@Indexed
@RestController
@RequestMapping(FileApiConstant.EXTERNAL_PREFIX)
@Tag(name = "文件外部接口", description = "供其他业务服务调用的文件 API")
@RequiredArgsConstructor
public class FileExternalController {

        private final IFileDomainService fileDomainService;

    @PostMapping(FileApiConstant.EXTERNAL_UPLOAD)
    @Operation(summary = "外部上传文件")
    public Response upload(@Valid @ModelAttribute FileExternalUploadDTO data,
                           @RequestPart("file") MultipartFile file) {
        return Response.success(fileDomainService.uploadExternal(data, file));
    }

    @PostMapping(FileApiConstant.EXTERNAL_SHA256)
    @Operation(summary = "计算文件 SHA-256")
    public Response sha256(@RequestPart("file") MultipartFile file) {
        return Response.success(fileDomainService.calculateSha256(file));
    }

    @GetMapping(FileApiConstant.EXTERNAL_FILE_ID)
    @Operation(summary = "查询外部文件详情")
    public Response detail(@NotBlank @PathVariable("fileId") String fileId) {
        return Response.success(fileDomainService.selectExternalByFileId(fileId));
    }

    @GetMapping(FileApiConstant.EXTERNAL_FILE_URL)
    @Operation(summary = "获取外部文件下载地址")
    public Response url(@NotBlank @PathVariable("fileId") String fileId) {
        return Response.success(fileDomainService.getDownloadUrl(fileId));
    }

    @DeleteMapping(FileApiConstant.EXTERNAL_FILE_ID)
    @Operation(summary = "删除外部文件")
    public Response delete(@NotBlank @PathVariable("fileId") String fileId) {
        fileDomainService.logicalDelete(fileId);
        return Response.success();
    }
}
