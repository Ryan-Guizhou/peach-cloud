package com.peach.fileservice.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.PeachGroup;
import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.dto.CloudStorageInstanceSaveDTO;
import com.peach.fileservice.qo.CloudStorageInstanceQO;
import com.peach.fileservice.service.ICloudStorageInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 云存储实例管理接口。
 *
 * <p>提供云存储实例的新增、修改、删除、启停、连通性测试和查询接口，仅负责实例配置管理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping(FileApiConstant.INTERNAL_STORAGE_INSTANCE_PREFIX)
@Tag(name = "CloudStorageInstanceController", description = "云存储实例管理")
@RequiredArgsConstructor
public class CloudStorageInstanceController {

        private final ICloudStorageInstanceService cloudStorageService;

    @PostMapping
    @Operation(summary = "新增云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增云存储实例,实例名称:['+#p0.instanceName+']'")
    public Response add(@Validated(PeachGroup.InsertGroup.class) @RequestBody CloudStorageInstanceSaveDTO data) {
        log.info("新增云存储实例,实例名称={}", data.getInstanceName());
        return Response.success(cloudStorageService.add(data));
    }

    @PutMapping
    @Operation(summary = "更新云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'更新云存储实例,实例ID:['+#p0.instanceId+'],实例名称:['+#p0.instanceName+']'")
    public Response update(@Validated(PeachGroup.UpdateGroup.class) @RequestBody CloudStorageInstanceSaveDTO data) {
        log.info("更新云存储实例,实例ID={},实例名称={}", data.getInstanceId(), data.getInstanceName());
        return Response.success(cloudStorageService.update(data));
    }

    @DeleteMapping("/{instanceId}")
    @Operation(summary = "删除云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除云存储实例,实例ID:['+#p0+']'")
    public Response delete(@NotBlank(message = "实例ID不能为空") @PathVariable String instanceId) {
        log.info("删除云存储实例,实例ID={}", instanceId);
        cloudStorageService.delete(instanceId);
        return Response.success();
    }

    @PostMapping("/{instanceId}/enable")
    @Operation(summary = "启用云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'启用云存储实例,实例ID:['+#p0+']'")
    public Response enable(@NotBlank(message = "实例ID不能为空") @PathVariable String instanceId) {
        log.info("启用云存储实例,实例ID={}", instanceId);
        cloudStorageService.enable(instanceId);
        return Response.success();
    }

    @PostMapping("/{instanceId}/disable")
    @Operation(summary = "禁用云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'禁用云存储实例,实例ID:['+#p0+']'")
    public Response disable(@NotBlank(message = "实例ID不能为空") @PathVariable String instanceId) {
        log.info("禁用云存储实例,实例ID={}", instanceId);
        cloudStorageService.disable(instanceId);
        return Response.success();
    }

    @PostMapping("/testConnection")
    @Operation(summary = "测试云存储连通性")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'测试云存储连通性,实例名称:['+#p0.instanceName+']'")
    public Response testConnection(@Validated(PeachGroup.InsertGroup.class) @RequestBody CloudStorageInstanceSaveDTO data) {
        log.info("测试云存储连通性,实例名称={}", data.getInstanceName());
        return Response.success(cloudStorageService.testConnection(data));
    }

    @GetMapping("/{instanceId}")
    @Operation(summary = "根据实例ID查询云存储实例")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'查询云存储实例,实例ID:['+#p0+']'")
    public Response selectById(@NotBlank(message = "实例ID不能为空") @PathVariable String instanceId) {
        log.info("查询云存储实例,实例ID={}", instanceId);
        return Response.success(cloudStorageService.selectById(instanceId));
    }

    @PostMapping("/list")
    @Operation(summary = "查询云存储实例列表")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'查询云存储实例列表'")
    public Response list(@RequestBody(required = false) CloudStorageInstanceQO data) {
        log.info("查询云存储实例列表,实例名称={},存储类型={},启用状态={}",
                data == null ? null : data.getInstanceName(),
                data == null ? null : data.getStoreType(),
                data == null ? null : data.getEnabled());
        return Response.success(cloudStorageService.list(data));
    }

    @GetMapping("/listEnabled")
    @Operation(summary = "查询已启用云存储实例列表")
    @UserOperLog(moduleCode = UserLogEnum.Module.FILESERVICE, optType = UserLogEnum.OptType.SELECT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'查询已启用云存储实例列表'")
    public Response listEnabled() {
        log.info("查询已启用云存储实例列表");
        return Response.success(cloudStorageService.listEnabled());
    }
}
