package com.peach.setting.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.setting.comon.enums.IpWhitelistGroup;
import com.peach.setting.dto.IpWhitelistDTO;
import com.peach.setting.qo.IpWhitelistQO;
import com.peach.setting.service.IIpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * IP 白名单管理接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Indexed
@Validated
@RestController
@RequestMapping("/setting/ipWhitelist")
@Tag(name = "IP白名单管理接口", description = "IP白名单管理接口")
public class IpWhitelistController {

    @Resource
    private IIpWhitelistService ipWhitelistService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询IP白名单")
    public Response pageList(@RequestBody IpWhitelistQO qo) {
        return Response.success(ipWhitelistService.pageList(qo));
    }

    @GetMapping("/selectById/{id}")
    @Operation(summary = "查询IP白名单详情")
    public Response selectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(ipWhitelistService.selectById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增IP白名单")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增IP白名单,IP:['+#p0.ipAddress+']'")
    public Response save(@Validated({IpWhitelistGroup.InsertGroup.class}) @RequestBody IpWhitelistDTO data) {
        ipWhitelistService.save(data);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新IP白名单")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新IP白名单,ID:['+#p0.id+'],IP:['+#p0.ipAddress+']'")
    public Response update(@Validated({IpWhitelistGroup.UpdatetGroup.class}) @RequestBody IpWhitelistDTO data) {
        ipWhitelistService.update(data);
        return Response.success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除IP白名单")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'删除IP白名单,ID集合数量:['+#p0.size()+']'")
    public Response delete(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        ipWhitelistService.delete(ids);
        return Response.success();
    }

    @PostMapping("/warmUp")
    @Operation(summary = "预热IP白名单缓存")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'预热IP白名单缓存'")
    public Response warmUp() {
        ipWhitelistService.warmUpCache();
        return Response.success();
    }
}
