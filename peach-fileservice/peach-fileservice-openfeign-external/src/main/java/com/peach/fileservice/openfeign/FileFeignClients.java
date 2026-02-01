package com.peach.fileservice.openfeign;

import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description 文件服务Feign客户端
 */
@FeignClient(
        name = ServiceNameConstant.FILE_SERVICE,
        path = ServicePathConstant.FILE_PATH_SERVICE,
        configuration = FeignConfigure.class
)
public interface FileFeignClients {

    /**
     * 文件上传接口
     * @param file 文件
     * @param targetPath 目标上传路径
     * @return
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("targetPath") String targetPath);


    /**
     * 文件下载接口
     * @param targetPath 源文件路径
     * @param localPath 本地保存路径
     * @param fileName 文件名
     * @return
     */
    @PostMapping(value = "/download/local")
    Response download(
            @RequestParam("targetPath") String targetPath,
            @RequestParam("localPath") String localPath,
            @RequestParam("fileName") String fileName);

    /**
     * 文件夹下载接口
     * @param sourceDir 源文件路径
     * @param localDir 本地保存路径
     * @return
     */
    @PostMapping(value = "/download/dir")
    Response downloadDir(
            @RequestParam("sourceDir") String sourceDir,
            @RequestParam("localDir") String localDir);


    /**
     * 删除文件接口
     * @param key
     * @return
     */
    @DeleteMapping("/delete")
    Response delete(@RequestParam("key") String key);

    /**
     * 获取代理后的文件url接口
     * @param key
     * @return
     */
    @GetMapping("/url")
    Response getUrl(@RequestParam("key") String key);

}
