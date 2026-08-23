package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.qo.AuthLogQO;
import com.peach.auth.service.IAuthLogService;
import com.peach.auth.vo.AuthLogVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权日志查询接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 21:10
 */
@Indexed
@RestController
@RequestMapping("/auth/authLog")
@Tag(name = "AuthLogController", description = "授权日志")
@RequiredArgsConstructor
public class AuthLogController {

        private final IAuthLogService authLogService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询授权日志")
    public Response pageList(@RequestBody AuthLogQO authLogQO) {
        PageInfo<AuthLogVO> pageInfo = authLogService.pageList(authLogQO);
        return Response.success(pageInfo);
    }
}
