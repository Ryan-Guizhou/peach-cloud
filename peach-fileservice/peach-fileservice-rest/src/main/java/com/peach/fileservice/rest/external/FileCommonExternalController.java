package com.peach.fileservice.rest.external;

import com.peach.common.response.Response;
import com.peach.fileservice.service.IFileStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @CreateTime 2026/2/1 14:45
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/file/external")
@Tag(name = "外部文件通用接口")
public class FileCommonExternalController {

    @Resource
    private IFileStoreService fileStoreService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件")
    public Response upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("targetPath") String targetPath) {
        try {
            String url = fileStoreService.upload(file.getInputStream(), targetPath, file.getOriginalFilename());
            return Response.success(url);
        } catch (Exception e) {
            log.error("upload error", e);
            return Response.fail("upload failed");
        }
    }

    @PostMapping("/upload/content")
    @Operation(summary = "上传文件内容")
    public Response uploadContent(
            @RequestParam("content") String content,
            @RequestParam("targetPath") String targetPath,
            @RequestParam("fileName") String fileName) {

        return Response.success(fileStoreService.upload(content, targetPath, fileName));
    }


    @PostMapping("/download/local")
    @Operation(summary = "下载文件")
    public Response downloadToLocal(
            @RequestParam("targetPath") String targetPath,
            @RequestParam("localPath") String localPath,
            @RequestParam("fileName") String fileName) {
        boolean download = fileStoreService.download(targetPath, localPath, fileName);
        log.info("download result:[{}] " + download);
        return download ? Response.success() : Response.fail();
    }

    @PostMapping("/download/dir")
    @Operation(summary = "下载某个文件夹下所有的文件")
    public Response downloadDir(
            @RequestParam("sourceDir") String sourceDir,
            @RequestParam("localDir") String localDir) {
        boolean flag = fileStoreService.downDir(sourceDir, localDir);
        return flag ? Response.success() : Response.fail();
    }


    @PostMapping("/copy/file")
    @Operation(summary = "复制文件")
    public Response copyFile(
            @RequestParam("sourcePath") String sourcePath,
            @RequestParam("targetPath") String targetPath) {
        boolean flag = fileStoreService.copyFile(sourcePath, targetPath);
        return flag ? Response.success() : Response.fail();
    }

    @PostMapping("/copy/dir")
    @Operation(summary = "复制文件夹")
    public Response copyDir(
            @RequestParam("sourceDir") String sourceDir,
            @RequestParam("targetDir") String targetDir) {
        boolean flag = fileStoreService.copyDir(sourceDir, targetDir);
        return flag ? Response.success() : Response.fail();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    public Response delete(@RequestParam("key") String key) {
        boolean flag = fileStoreService.delete(key);
        return flag ? Response.success() : Response.fail();
    }


    @GetMapping("/url")
    @Operation(summary = "获取文件url")
    public Response getUrl(@RequestParam("key") String key) {
        String urlByKey = fileStoreService.getPathByKey(key);
        return Response.success(urlByKey);
    }

    @GetMapping("/path")
    @Operation(summary = "获取文件路径")
    public Response getPath(@RequestParam("key") String key) {
        return Response.success(fileStoreService.getPathByKey(key));
    }


    @PostMapping("/acl/public-read")
    @Operation(summary = "为文件设置公共读")
    public Response setPublicRead(@RequestParam("path") String path) {
        fileStoreService.setPublicReadAcl(path);
        return Response.success();
    }

}
