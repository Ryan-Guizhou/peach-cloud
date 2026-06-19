package com.peach.setting.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.setting.comon.enums.ValueSetGroup;
import com.peach.setting.comon.enums.ValueSetItemGroup;
import com.peach.setting.dto.ValueSetDTO;
import com.peach.setting.dto.ValueSetItemDTO;
import com.peach.setting.qo.ValueSetItemQO;
import com.peach.setting.qo.ValueSetQO;
import com.peach.setting.service.IValueSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 20:51
 * @Description 值集管理接口
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/setting/valueSet")
@Tag(name = "值集管理接口", description = "值集管理接口")
public class ValueSetController {

    @Resource
    private IValueSetService valueSetService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询值集")
    public Response pageList(@RequestBody ValueSetQO qo) {
        return Response.success(valueSetService.pageList(qo));
    }

    @GetMapping("/selectById/{id}")
    @Operation(summary = "查询值集详情")
    public Response selectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(valueSetService.selectById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增值集")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增值集:['+#p0+']'")
    public Response save(@Validated({ValueSetGroup.InsertGroup.class}) @RequestBody ValueSetDTO data) {
        valueSetService.save(data);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新值集")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新值集:['+#p0+']'")
    public Response update(@Validated({ValueSetGroup.UpdatetGroup.class}) @RequestBody ValueSetDTO data) {
        valueSetService.update(data);
        return Response.success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除值集")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除值集,值集ID集合:['+#p0+']'")
    public Response delete(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        valueSetService.delete(ids);
        return Response.success();
    }

    @PostMapping("/item/pageList")
    @Operation(summary = "分页查询值集项")
    public Response itemPageList(@RequestBody ValueSetItemQO qo) {
        return Response.success(valueSetService.itemPageList(qo));
    }

    @GetMapping("/item/selectById/{id}")
    @Operation(summary = "查询值集项详情")
    public Response itemSelectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(valueSetService.itemSelectById(id));
    }

    @GetMapping("/item/list/{valueSetCode}")
    @Operation(summary = "根据值集编码查询值集项列表")
    public Response itemList(@NotBlank(message = "值集编码不能为空") @PathVariable String valueSetCode) {
        return Response.success(valueSetService.itemListByValueSetCode(valueSetCode));
    }

    @PostMapping("/item/save")
    @Operation(summary = "新增值集项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增值集项:['+#p0+']'")
    public Response saveItem(@Validated({ValueSetItemGroup.InsertGroup.class}) @RequestBody ValueSetItemDTO data) {
        valueSetService.saveItem(data);
        return Response.success();
    }

    @PostMapping("/item/update")
    @Operation(summary = "更新值集项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新值集项:['+#p0+']'")
    public Response updateItem(@Validated({ValueSetItemGroup.UpdatetGroup.class}) @RequestBody ValueSetItemDTO data) {
        valueSetService.updateItem(data);
        return Response.success();
    }

    @DeleteMapping("/item/delete")
    @Operation(summary = "删除值集项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除值集项,值集项ID集合:['+#p0+']'")
    public Response deleteItem(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        valueSetService.deleteItem(ids);
        return Response.success();
    }
}
