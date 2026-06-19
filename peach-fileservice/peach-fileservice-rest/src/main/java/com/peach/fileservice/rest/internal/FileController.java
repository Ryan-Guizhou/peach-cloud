package com.peach.fileservice.rest.internal;

import com.peach.common.response.Response;
import com.peach.fileservice.dto.FileMultipartCompleteDTO;
import com.peach.fileservice.dto.FileMultipartInitDTO;
import com.peach.fileservice.dto.FileMultipartPartUrlDTO;
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

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

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
@RequestMapping("/file/internal/")
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
     * 初始化分片上传
     *
     * <p>创建分片上传会话，返回 uploadId 和预签名URL列表，
     * 客户端根据返回的URL列表逐个上传分片。</p>
     *
     * @param data 分片上传初始化数据传输对象
     * @return 统一响应结果，包含分片上传初始化结果
     */
    @PostMapping("/multipart/init")
    @Operation(summary = "初始化分片上传")
    public Response initMultipart(@Valid @RequestBody FileMultipartInitDTO data) {
        return Response.success(fileDomainService.initMultipartUpload(data));
    }

    /**
     * 获取分片上传地址
     *
     * <p>为指定分片生成预签名上传URL，用于客户端直传分片数据。</p>
     *
     * @param data 分片URL数据传输对象
     * @return 统一响应结果，包含分片URL结果
     */
    @PostMapping("/multipart/part-url")
    @Operation(summary = "获取分片上传地址")
    public Response partUrl(@Valid @RequestBody FileMultipartPartUrlDTO data) {
        return Response.success(fileDomainService.prepareMultipartPart(data));
    }

    /**
     * 完成分片上传
     *
     * <p>所有分片上传完成后调用此接口，合并分片并创建文件记录。</p>
     *
     * @param data 分片完成数据传输对象
     * @return 统一响应结果，包含文件上传结果
     */
    @PostMapping("/multipart/complete")
    @Operation(summary = "完成分片上传")
    public Response completeMultipart(@Valid @RequestBody FileMultipartCompleteDTO data) {
        return Response.success(fileDomainService.completeMultipartUpload(data));
    }

    /**
     * 中止分片上传
     *
     * <p>取消正在进行的分片上传任务，清理已上传的分片和会话数据。</p>
     *
     * @param sessionId 上传会话ID（不能为空）
     * @return 统一响应结果
     */
    @PostMapping("/multipart/abort/{sessionId}")
    @Operation(summary = "中止分片上传")
    public Response abortMultipart(@NotBlank(message = "sessionId不能为空") @PathVariable String sessionId) {
        fileDomainService.abortMultipartUpload(sessionId);
        return Response.success();
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

    /**
     * 主方法（仅用于测试）
     *
     * <p>计算指定文件的 SHA-256 哈希值，用于测试和调试。</p>
     *
     * @param args 命令行参数
     * @throws Exception 文件读取或哈希计算异常
     */
    public static void main(String[] args) throws Exception {
        String sha256 = sha256(Paths.get("C:\\Users\\Administrator\\Downloads\\peach-storage.rar"));
        System.out.println("sha256 = " + sha256);
    }

    /**
     * 计算文件的 SHA-256 哈希值
     *
     * <p>使用流式读取方式计算大文件的哈希值，避免内存溢出。</p>
     *
     * @param path 文件路径
     * @return SHA-256 哈希值（十六进制字符串）
     * @throws Exception 文件读取或哈希计算异常
     */
    public static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        byte[] hash = digest.digest();
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
    }
}
