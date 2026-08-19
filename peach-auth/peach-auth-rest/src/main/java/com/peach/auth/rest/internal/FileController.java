package com.peach.auth.rest.internal;

import com.peach.common.response.Response;
import com.peach.fileservice.openfeign.FileFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import jakarta.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth/file")
@Tag(name = "FileController", description = "资源管理")
public class FileController {

    @Resource
    private FileFeignClient fileFeignClient;

    @PostMapping("/upload")
    public Response upload(@RequestPart("file") MultipartFile file,
                           @RequestParam("bizType") String bizType,
                           @RequestParam(value = "bizId", required = false) String bizId,
                           @RequestParam(value = "bizTag", required = false) String bizTag,
                           @RequestParam(value = "displayName", required = false) String displayName,
                           @RequestParam(value = "contentType", required = false) String contentType,
                           @RequestParam(value = "remark", required = false) String remark,
                           @RequestParam(value = "storageProvider", required = false) String storageProvider) {
        return fileFeignClient.upload(file, bizType, bizId, bizTag, displayName, contentType, remark, storageProvider);
    }

    @PostMapping("/url")
    @Operation(summary = "获取文件 URL")
    public Response getUrl(@RequestParam("fileId") String fileId) {
        return fileFeignClient.getUrl(fileId);
    }

    @PostMapping(value = "/sha256", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "计算文件 SHA-256")
    public Response sha256(@RequestPart("file") MultipartFile file) {
        return fileFeignClient.sha256(file);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    public Response delete(@RequestParam("fileId") String fileId) {
        return fileFeignClient.delete(fileId);
    }
}
