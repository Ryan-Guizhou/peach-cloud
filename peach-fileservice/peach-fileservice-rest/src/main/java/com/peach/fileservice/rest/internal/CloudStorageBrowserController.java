package com.peach.fileservice.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.dto.CloudStorageCreateDirectoryDTO;
import com.peach.fileservice.dto.CloudStorageDeleteDirectoryDTO;
import com.peach.fileservice.dto.CloudStorageDeleteObjectDTO;
import com.peach.fileservice.dto.CloudStorageListDTO;
import com.peach.fileservice.dto.CloudStorageUploadDTO;
import com.peach.fileservice.service.ICloudStorageBrowserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/**
 * 云存储浏览。
 * <p>提供云存储目录浏览、对象元数据查询、上传、创建目录以及删除对象和目录等接口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping(FileApiConstant.INTERNAL_STORAGE_BROWSER_PREFIX)
@Tag(name = "CloudStorageBrowserController", description = "云存储浏览")
@RequiredArgsConstructor
public class CloudStorageBrowserController {

        private final ICloudStorageBrowserService cloudStorageBrowserService;

    @GetMapping("/{instanceId}/bucket-exists")
    @Operation(summary = "检查存储桶是否存在")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'检查存储桶是否存在,实例ID:['+#p0+']'")
    public Response bucketExists(@PathVariable("instanceId") String instanceId) {
        log.info("Checking bucket existence, instanceId={}", instanceId);
        return Response.success(cloudStorageBrowserService.bucketExists(instanceId));
    }

    @GetMapping("/{instanceId}/object-exists")
    @Operation(summary = "检查对象是否存在")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'检查对象是否存在,实例ID:['+#p0+'],对象Key:['+#p1+']'")
    public Response objectExists(@PathVariable("instanceId") String instanceId,
                                 @RequestParam("objectKey") String objectKey) {
        log.info("Checking object existence, instanceId={}, objectKey={}", instanceId, objectKey);
        return Response.success(cloudStorageBrowserService.objectExists(instanceId, objectKey));
    }

    @PostMapping("/{instanceId}/list")
    @Operation(summary = "查询对象列表")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'查询对象列表,实例ID:['+#p0+'],目录路径:['+#p1.path+']'")
    public Response list(@PathVariable("instanceId") String instanceId,
                         @RequestBody(required = false) CloudStorageListDTO data) {
        log.info("Listing cloud storage objects, instanceId={}, path={}", instanceId, data == null ? null : data.getPath());
        return Response.success(cloudStorageBrowserService.list(instanceId, data));
    }

    @GetMapping("/{instanceId}/stat")
    @Operation(summary = "查询对象元数据")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'查询对象元数据,实例ID:['+#p0+'],对象Key:['+#p1+']'")
    public Response stat(@PathVariable("instanceId") String instanceId,
                         @RequestParam("objectKey") String objectKey) {
        log.info("Fetching object metadata, instanceId={}, objectKey={}", instanceId, objectKey);
        return Response.success(cloudStorageBrowserService.stat(instanceId, objectKey));
    }

    @PostMapping("/{instanceId}/upload")
    @Operation(summary = "上传对象")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'上传云存储对象,实例ID:['+#p0+'],目标路径:['+#p1.targetPath+']'")
    public Response upload(@PathVariable("instanceId") String instanceId,
                           @Valid @ModelAttribute CloudStorageUploadDTO data,
                           @RequestPart("file") MultipartFile file) {
        log.info("Uploading cloud storage object, instanceId={}, targetPath={}, filename={}", instanceId,
                data == null ? null : data.getTargetPath(), file == null ? null : file.getOriginalFilename());
        return Response.success(cloudStorageBrowserService.upload(instanceId, data.getTargetPath(), file));
    }

    @PostMapping("/{instanceId}/mkdir")
    @Operation(summary = "创建目录")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'创建云存储目录,实例ID:['+#p0+'],目录路径:['+#p1.path+']'")
    public Response createDirectory(@PathVariable("instanceId") String instanceId,
                                    @Valid @RequestBody CloudStorageCreateDirectoryDTO data) {
        log.info("Creating cloud storage directory, instanceId={}, path={}", instanceId, data.getPath());
        cloudStorageBrowserService.createDirectory(instanceId, data.getPath());
        return Response.success();
    }

    @PostMapping("/{instanceId}/delete-object")
    @Operation(summary = "删除对象")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除云存储对象,实例ID:['+#p0+'],对象Key:['+#p1.objectKey+']'")
    public Response deleteObject(@PathVariable("instanceId") String instanceId,
                                 @Valid @RequestBody CloudStorageDeleteObjectDTO data) {
        log.info("Deleting cloud storage object, instanceId={}, objectKey={}", instanceId, data.getObjectKey());
        cloudStorageBrowserService.deleteObject(instanceId, data.getObjectKey());
        return Response.success();
    }

    @PostMapping("/{instanceId}/delete-directory")
    @Operation(summary = "删除目录")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除云存储目录,实例ID:['+#p0+'],目录路径:['+#p1.path+']'")
    public Response deleteDirectory(@PathVariable("instanceId") String instanceId,
                                    @Valid @RequestBody CloudStorageDeleteDirectoryDTO data) {
        log.info("Deleting cloud storage directory, instanceId={}, path={}", instanceId, data.getPath());
        cloudStorageBrowserService.deleteDirectory(instanceId, data.getPath());
        return Response.success();
    }
}
