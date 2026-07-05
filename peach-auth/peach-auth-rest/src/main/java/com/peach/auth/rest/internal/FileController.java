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

import javax.annotation.Resource;

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
                           @RequestParam("targetPath") String targetPath) {
        return fileFeignClient.upload(file, targetPath);
    }

    @PostMapping("/download/local")
    @Operation(summary = "下载文件")
    public Response downloadToLocal(
            @RequestParam("targetPath") String targetPath,
            @RequestParam("localPath") String localPath,
            @RequestParam("fileName") String fileName) {
        return fileFeignClient.download(targetPath, localPath, fileName);
    }


    @PostMapping("/download/dir")
    @Operation(summary = "下载文件夹")
    public Response downloadDir(@RequestParam("sourceDir") String sourceDir,
                                @RequestParam("localDir") String localDir){
        return fileFeignClient.downloadDir(sourceDir, localDir);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    public Response delete(@RequestParam("key") String key) {
        return fileFeignClient.delete(key);
    }

    @PostMapping("/url")
    @Operation(summary = "获取文件url")
    public Response getUrl(@RequestParam("key") String key) {
        return fileFeignClient.getUrl(key);
    }
}
