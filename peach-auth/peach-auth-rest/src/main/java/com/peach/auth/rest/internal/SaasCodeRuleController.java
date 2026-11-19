package com.peach.auth.rest.internal;

import com.peach.auth.dto.SaasCodeRuleFormatDTO;
import com.peach.auth.dto.SaasCodeRuleStatusDTO;
import com.peach.auth.dto.SaasCodeRuleWatermarkDTO;
import com.peach.auth.service.SaasCodeRuleManagementService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SaaS 当前物理租户的受保护编码规则目录与维护入口。
 *
 * <p>由 SaasAuthSessionFilter 验证 Same-Token、终端用户和实时授权版本；
 * Service 只能使用当前 Token 确认的平台或租户命名空间，不接受请求自报身份。
 * 仅支持已有规则，不提供匿名开通或新建任意发号规则。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/code-rules")
@Tag(name = "SaasCodeRuleController", description = "受信业务编码规则管理")
public class SaasCodeRuleController {

    private final SaasCodeRuleManagementService codeRuleService;

    @GetMapping
    @Operation(summary = "查询当前平台或租户命名空间的编码规则")
    public Response list(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int pageSize) {
        return Response.success(codeRuleService.list(page, pageSize));
    }

    @GetMapping("/{prefix}")
    @Operation(summary = "读取当前命名空间已有规则")
    public Response get(@NotBlank @PathVariable String prefix) {
        return Response.success(codeRuleService.get(prefix));
    }

    @GetMapping("/{prefix}/preview")
    @Operation(summary = "无副作用预览下一候选编号，不预留水位")
    public Response preview(@NotBlank @PathVariable String prefix) {
        return Response.success(codeRuleService.preview(prefix));
    }

    @PutMapping("/{prefix}/status")
    @Operation(summary = "按预期版本启用或停用规则")
    public Response changeStatus(@NotBlank @PathVariable String prefix,
                                 @Valid @RequestBody SaasCodeRuleStatusDTO request) {
        codeRuleService.changeStatus(prefix, request.getEnabled(), request.getExpectedVersion());
        return Response.success();
    }

    @PutMapping("/{prefix}/format")
    @Operation(summary = "仅未发号规则可修改分隔符及编号宽度")
    public Response changeFormat(@NotBlank @PathVariable String prefix,
                                 @Valid @RequestBody SaasCodeRuleFormatDTO request) {
        codeRuleService.changeFormat(prefix, request.getSeparator(),
                request.getWidth(), request.getExpectedVersion());
        return Response.success();
    }

    @PostMapping("/{prefix}/watermark")
    @Operation(summary = "经数据迁移校验后只允许向前修正发号水位")
    public Response advanceWatermark(@NotBlank @PathVariable String prefix,
                                     @Valid @RequestBody SaasCodeRuleWatermarkDTO request) {
        codeRuleService.advanceWatermark(prefix, request.getWatermark(), request.getExpectedVersion());
        return Response.success();
    }
}
