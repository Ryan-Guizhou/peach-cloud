package com.peach.fileservice.rest.internal;

import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @GetMapping("")
    @Operation(summary = "文件服务通用接口")
    public Response common() {
        log.info("文件服务通用接口");
        return Response.success();
    }
}
