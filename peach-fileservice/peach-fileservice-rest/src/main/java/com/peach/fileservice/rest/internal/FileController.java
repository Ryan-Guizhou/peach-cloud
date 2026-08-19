package com.peach.fileservice.rest.internal;

import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.dto.FileUploadCheckDTO;
import com.peach.fileservice.qo.FileQueryQO;
import com.peach.fileservice.service.IFileDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 文件域 REST 控制器
 *
 * <p>提供文件上传、下载、分片上传、文件管理等 RESTful API 接口。
 * 支持秒传检测、分片上传、逻辑删除、文件恢复等高级功能。</p>
 *
 * <p>接口路径前缀：{@code /file/internal/}</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Indexed
@Validated
@RestController
@RequestMapping(FileApiConstant.INTERNAL_PREFIX)
@Tag(name = "文件领域接口", description = "文件上传下载与文件记录管理接口")
public class FileController {

    @Resource
    private IFileDomainService fileDomainService;

    /**
     * 文件上传预检查
     *
     * <p>检查文件是否已存在（秒传检测），验证文件合法性，返回上传会话信息。</p>
     *
     * @param data 文件上传校验数据传输对象
     * @return 统一响应结果，包含文件上传校验结果
     */
    @PostMapping("/upload/check")
    @Operation(summary = "文件上传预检查")
    public Response uploadCheck(@Valid @RequestBody FileUploadCheckDTO data) {
        return Response.success(fileDomainService.uploadCheck(data));
    }

    /**
     * 普通文件上传
     *
     * <p>适用于小文件的直接上传，一次性完成文件传输。</p>
     *
     * @param data 文件上传校验数据传输对象
     * @param file 上传的文件对象
     * @return 统一响应结果，包含文件上传结果
     */
    @PostMapping("/upload")
    @Operation(summary = "普通文件上传")
    public Response upload(@Valid @ModelAttribute FileUploadCheckDTO data,
                           @RequestPart("file") MultipartFile file) {
        return Response.success(fileDomainService.upload(data, file));
    }

    /**
     * 查询文件详情
     *
     * <p>根据文件ID查询文件记录的详细信息。</p>
     *
     * @param fileId 文件ID（不能为空）
     * @return 统一响应结果，包含文件记录详情
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件详情")
    public Response selectByFileId(@NotBlank(message = "fileId不能为空") @PathVariable String fileId) {
        return Response.success(fileDomainService.selectByFileId(fileId));
    }

    /**
     * 获取文件下载地址
     *
     * <p>生成文件的预签名下载URL，支持设置过期时间。</p>
     *
     * @param fileId 文件ID（不能为空）
     * @return 统一响应结果，包含文件下载URL结果
     */
    @GetMapping("/{fileId}/url")
    @Operation(summary = "获取文件下载地址")
    public Response getDownloadUrl(@NotBlank(message = "fileId不能为空") @PathVariable String fileId) {
        return Response.success(fileDomainService.getDownloadUrl(fileId));
    }

    /**
     * 分页查询业务文件
     *
     * <p>根据查询条件分页查询文件记录列表，支持按文件名、类型、时间等条件查询。</p>
     *
     * @param qo 文件查询参数对象
     * @return 统一响应结果，包含文件记录分页结果
     */
    @PostMapping("/pageList")
    @Operation(summary = "分页查询业务文件")
    public Response pageList(@RequestBody FileQueryQO qo) {
        return Response.success(fileDomainService.pageList(qo));
    }

    /**
     * 逻辑删除文件
     *
     * <p>将文件标记为已删除状态，不立即从存储中移除，支持在保留期内恢复。</p>
     *
     * @param fileId 文件ID（不能为空）
     * @return 统一响应结果
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "逻辑删除文件")
    public Response delete(@NotBlank(message = "fileId不能为空") @PathVariable String fileId) {
        fileDomainService.logicalDelete(fileId);
        return Response.success();
    }

    /**
     * 恢复逻辑删除文件
     *
     * <p>将逻辑删除的文件恢复为正常状态，仅在保留期内有效。</p>
     *
     * @param fileId 文件ID（不能为空）
     * @return 统一响应结果
     */
    @PostMapping("/{fileId}/restore")
    @Operation(summary = "恢复逻辑删除文件")
    public Response restore(@NotBlank(message = "fileId不能为空") @PathVariable String fileId) {
        fileDomainService.restore(fileId);
        return Response.success();
    }

}
