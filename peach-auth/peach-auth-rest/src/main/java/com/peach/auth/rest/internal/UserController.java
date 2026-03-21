package com.peach.auth.rest.internal;




import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 11:41
 */
@Tag(name = "UserController",description = "用户管理")
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth")
public class UserController {

    @Operation(summary = "查询用户列表")
    @PostMapping("/query")
    public Response query() {
        return Response.success();
    }

}