package com.peach.fileservice.rest.internal;

import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.service.IFileStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/25 16:45
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/file/common")
@Tag(name = "FileserviceController", description = "文件服务通用接口")
public class FileserviceController {

    @Resource
    private IFileStoreService fileStoreService;

    public FileserviceController(IFileStoreService fileStoreService) {
        this.fileStoreService = fileStoreService;
    }


    @GetMapping("")
    @Operation(summary = "文件服务通用接口")
    public Response common() {
        log.info("文件服务通用接口");
        return Response.success();
    }


    @PostMapping("")
    @Operation(summary = "文件上传")
    public Response upload(@RequestPart("file") MultipartFile file) throws IOException {
        log.info("文件服务通用接口");
        File targetFile = convertMultipartFileToFile(file);
        String name = file.getName();
        String upload = fileStoreService.upload(targetFile, "/peach/common", "12312312".concat(file.getOriginalFilename()));
        return Response.success(upload);
    }

    @GetMapping("/download")
    @Operation(summary = "根据key下载")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String key, @RequestParam String fileName) throws IOException {
        if (StringUtil.isBlank(fileName)) {
            fileName = StringUtil.getStringValue(System.currentTimeMillis());
        }
        InputStream inputStream = fileStoreService.getInputStreamByKey(key);
        // 2. 流式写入响应体
        StreamingResponseBody stream = outputStream -> {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            inputStream.close();
        };
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(fileName, PubCommonConst.UTF_8) + "\"")
                .body(stream);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "根据key删除文件")
    public Response upload(String key) {
        try {
            fileStoreService.delete(key);
        }catch (Exception ex){
            throw new RuntimeException("上传文件失败");
        }
        return Response.success().setMsg("文件删除成功");
    }

    public File convertMultipartFileToFile(MultipartFile file) throws IOException {
        // 获取文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名为空！");
        }

        // 创建临时文件
        File convFile = File.createTempFile("temp-", originalFilename);

        // 将 MultipartFile 转换为 File
        file.transferTo(convFile);

        // 关闭 JVM 退出时自动删除（可选）
        convFile.deleteOnExit();

        return convFile;
    }
}
