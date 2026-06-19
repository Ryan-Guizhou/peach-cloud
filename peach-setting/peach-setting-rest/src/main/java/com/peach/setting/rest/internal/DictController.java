package com.peach.setting.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.setting.comon.enums.DictItemGroup;
import com.peach.setting.comon.enums.DictTypeGroup;
import com.peach.setting.dto.DictItemDTO;
import com.peach.setting.dto.DictTypeDTO;
import com.peach.setting.qo.DictItemQO;
import com.peach.setting.qo.DictTypeQO;
import com.peach.setting.service.IDictService;
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
 * @Description 字典管理接口
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/setting/dict")
@Tag(name = "字典管理接口", description = "字典管理接口")
public class DictController {

    @Resource
    private IDictService dictService;

    @PostMapping("/type/pageList")
    @Operation(summary = "分页查询字典类型")
    public Response typePageList(@RequestBody DictTypeQO qo) {
        return Response.success(dictService.typePageList(qo));
    }

    @GetMapping("/type/selectById/{id}")
    @Operation(summary = "查询字典类型详情")
    public Response typeSelectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(dictService.typeSelectById(id));
    }

    @PostMapping("/type/save")
    @Operation(summary = "新增字典类型")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增字典类型:['+#p0+']'")
    public Response saveType(@Validated({DictTypeGroup.InsertGroup.class}) @RequestBody DictTypeDTO data) {
        dictService.saveType(data);
        return Response.success();
    }

    @PostMapping("/type/update")
    @Operation(summary = "更新字典类型")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新字典类型:['+#p0+']'")
    public Response updateType(@Validated({DictTypeGroup.UpdatetGroup.class}) @RequestBody DictTypeDTO data) {
        dictService.updateType(data);
        return Response.success();
    }

    @DeleteMapping("/type/delete")
    @Operation(summary = "删除字典类型")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除字典类型,字典类型ID集合:['+#p0+']'")
    public Response deleteType(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        dictService.deleteType(ids);
        return Response.success();
    }

    @PostMapping("/item/pageList")
    @Operation(summary = "分页查询字典项")
    public Response itemPageList(@RequestBody DictItemQO qo) {
        return Response.success(dictService.itemPageList(qo));
    }

    @GetMapping("/item/selectById/{id}")
    @Operation(summary = "查询字典项详情")
    public Response itemSelectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(dictService.itemSelectById(id));
    }

    @GetMapping("/item/list/{dictCode}")
    @Operation(summary = "根据字典编码查询字典项列表")
    public Response itemList(@NotBlank(message = "字典编码不能为空") @PathVariable String dictCode) {
        return Response.success(dictService.itemListByDictCode(dictCode));
    }

    @PostMapping("/item/save")
    @Operation(summary = "新增字典项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增字典项:['+#p0+']'")
    public Response saveItem(@Validated({DictItemGroup.InsertGroup.class}) @RequestBody DictItemDTO data) {
        dictService.saveItem(data);
        return Response.success();
    }

    @PostMapping("/item/update")
    @Operation(summary = "更新字典项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新字典项:['+#p0+']'")
    public Response updateItem(@Validated({DictItemGroup.UpdatetGroup.class}) @RequestBody DictItemDTO data) {
        dictService.updateItem(data);
        return Response.success();
    }

    @DeleteMapping("/item/delete")
    @Operation(summary = "删除字典项")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除字典项,字典项ID集合:['+#p0+']'")
    public Response deleteItem(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        dictService.deleteItem(ids);
        return Response.success();
    }
}
