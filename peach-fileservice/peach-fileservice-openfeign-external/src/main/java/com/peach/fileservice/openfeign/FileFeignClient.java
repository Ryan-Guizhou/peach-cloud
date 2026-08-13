package com.peach.fileservice.openfeign;

import org.springframework.stereotype.Indexed;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import com.peach.fileservice.common.FileApiConstant;
import com.peach.fileservice.openfeign.fallback.FileFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description 文件服务Feign客户端
 */
@Indexed
@FeignClient(
        contextId = ServiceContextConstant.FILE_SERVICE_CONTEXT,
        name = ServiceNameConstant.FILE_SERVICE,
        path = ServicePathConstant.FILE_PATH_SERVICE,
        fallbackFactory = FileFeignClientFallbackFactory.class
)
public interface FileFeignClient {

    /**
     * 外部文件上传接口。服务端根据文件内容计算 SHA-256。
     *
     * <p>大文件（建议超过 {@code peach.openfeign.upload-max-bytes}，默认 10MB）应改用对象存储直传，
     * 避免业务服务经 Feign 中转导致内存与超时风险。</p>
     *
     * @param file 文件
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param bizTag 业务标签
     * @param displayName 显示文件名
     * @param contentType 内容类型
     * @param remark 备注
     * @param storageProvider 存储提供方
     * @return 上传结果
     */
    @PostMapping(value = FileApiConstant.EXTERNAL_UPLOAD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("bizType") String bizType,
            @RequestParam(value = "bizId", required = false) String bizId,
            @RequestParam(value = "bizTag", required = false) String bizTag,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "storageProvider", required = false) String storageProvider);

    /**
     * 计算文件 SHA-256 摘要。
     *
     * @param file 文件
     * @return 摘要结果
     */
    @PostMapping(value = FileApiConstant.EXTERNAL_SHA256, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response sha256(@RequestPart("file") MultipartFile file);

    /**
     * 查询文件详情。
     *
     * @param fileId 业务文件ID
     * @return 外部文件详情
     */
    @GetMapping(FileApiConstant.EXTERNAL_FILE_ID)
    Response detail(@PathVariable("fileId") String fileId);

    /**
     * 获取临时下载地址。
     *
     * @param fileId 业务文件ID
     * @return 下载地址
     */
    @GetMapping(FileApiConstant.EXTERNAL_FILE_URL)
    Response getUrl(@PathVariable("fileId") String fileId);

    /**
     * 逻辑删除文件。
     *
     * @param fileId 业务文件ID
     * @return 操作结果
     */
    @DeleteMapping(FileApiConstant.EXTERNAL_FILE_ID)
    Response delete(@PathVariable("fileId") String fileId);

}
