package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.core.IUserOperLogService;
import com.peach.auth.qo.UserOperLogQO;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 22:55
 */
@Indexed
@RestController
@RequestMapping("/auth/operLog")
@Tag(name = "UserOperLogController", description = "操作日志")
@RequiredArgsConstructor
public class UserOperLogController {

        private final IUserOperLogService userOperLogService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询操作日志")
    public Response pageList(@RequestBody UserOperLogQO userOperLogQO) {
        PageInfo<UserOperLogVO> pageInfo = userOperLogService.pageList(userOperLogQO);
        return Response.success(pageInfo);
    }
}
