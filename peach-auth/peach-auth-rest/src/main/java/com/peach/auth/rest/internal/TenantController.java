package com.peach.auth.rest.internal;
import com.peach.auth.dto.TenantDTO;
import com.peach.auth.group.TenantGroup;
import com.peach.auth.qo.TenantQO;
import com.peach.auth.service.ITenantService;
import com.peach.common.response.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@RestController
@Validated
@RequestMapping("/auth/tenant")
@RequiredArgsConstructor
public class TenantController {
    private final ITenantService tenantService;
    @PostMapping("/pageList")
    public Response pageList(@Valid @RequestBody TenantQO query) { return Response.success(tenantService.pageList(query)); }
    @GetMapping("/selectById")
    public Response selectById(@RequestParam @NotBlank @Size(max = 36) String id) { return Response.success(tenantService.selectById(id)); }
    @PostMapping("/add")
    public Response add(@Validated(TenantGroup.Create.class) @RequestBody TenantDTO input) {
        tenantService.add(input); return Response.success();
    }
    @PostMapping("/update")
    public Response update(@Validated(TenantGroup.Update.class) @RequestBody TenantDTO input) {
        tenantService.update(input); return Response.success();
    }
    @PostMapping("/status")
    public Response status(@Valid @RequestBody StatusCommand input) {
        tenantService.status(input.getId(), input.getVersion(), input.getStatus()); return Response.success();
    }
    @DeleteMapping("/delete")
    public Response delete(@RequestParam @NotBlank @Size(max = 36) String id, @RequestParam @PositiveOrZero Long version) {
        tenantService.delete(id, version); return Response.success();
    }
    /**
     * 租户管理。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/21 18:00
     */
    @Data
    public static class StatusCommand {
        @NotBlank @Size(max = 36)
        private String id;
        @NotNull @PositiveOrZero
        private Long version;
        @NotBlank @Pattern(regexp = "PENDING|ACTIVE|SUSPENDED|CLOSED")
        private String status;
    }
}
