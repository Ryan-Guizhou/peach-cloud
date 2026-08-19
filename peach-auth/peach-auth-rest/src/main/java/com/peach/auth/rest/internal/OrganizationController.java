package com.peach.auth.rest.internal;

import com.github.pagehelper.PageInfo;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.OrganizationDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.group.OrganizationGroup;
import com.peach.auth.qo.OrganizationQO;
import com.peach.auth.service.IOrganizationService;
import com.peach.auth.vo.OrganizationVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;

/**
 * 机构管理接口。
 *
 * <p>提供机构的分页查询、单条查询、创建、删除和更新接口，仅处理机构自身数据，不负责租户切换逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/organization")
@Tag(name = "OrganizationController", description = "机构管理")
public class OrganizationController {

    @Resource
    private IOrganizationService organizationService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询机构")
    public Response pageList(@RequestBody OrganizationQO organizationQO) {
        PageInfo<OrganizationVO> pageInfo = organizationService.pageList(organizationQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/selectById")
    @Operation(summary = "根据机构ID查询机构")
    public Response selectById(@NotBlank(message = "机构ID不能为空") String orgId) {
        OrganizationVO organizationVO = organizationService.selectById(orgId);
        return Response.success(organizationVO);
    }

    @PostMapping("/add")
    @Operation(summary = "新增机构")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增机构信息,机构信息:['+#p0+']'")
    public Response add(@Validated({OrganizationGroup.insertGroup.class}) @RequestBody OrganizationDTO organizationDTO) {
        log.info("新增机构,机构编码={}", organizationDTO.getOrgCode());
        organizationService.add(organizationDTO);
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "根据机构ID删除机构")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除机构信息,机构ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "机构ID不能为空") String orgId) {
        log.info("删除机构,机构ID={}", orgId);
        organizationService.delById(orgId);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新机构")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新机构信息,机构信息:['+#p0+']'")
    public Response update(@Validated({OrganizationGroup.updateGroup.class}) @RequestBody OrganizationDTO organizationDTO) {
        log.info("更新机构,机构ID={}", organizationDTO.getOrgId());
        organizationService.update(organizationDTO);
        return Response.success();
    }
}
