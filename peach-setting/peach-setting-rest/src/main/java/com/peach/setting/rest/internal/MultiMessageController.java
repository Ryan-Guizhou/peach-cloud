package com.peach.setting.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.setting.comon.enums.LanguageGroup;
import com.peach.setting.comon.enums.MultiMessageGroup;
import com.peach.setting.dto.LanguageDTO;
import com.peach.setting.dto.MultiMessageDTO;
import com.peach.setting.qo.LanguageQO;
import com.peach.setting.qo.MulitMessageQO;
import com.peach.setting.service.IMultiMessageService;
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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 20:51
 * @Description 多语言管理接口
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/setting/multiMessage")
@Tag(name = "多语言管理接口", description = "多语言管理接口")
@RequiredArgsConstructor
public class MultiMessageController {

        private final IMultiMessageService i18nService;

    @PostMapping("/language/pageList")
    @Operation(summary = "分页查询语言配置")
    public Response languagePageList(@RequestBody LanguageQO qo) {
        return Response.success(i18nService.languagePageList(qo));
    }

    @GetMapping("/language/selectById/{id}")
    @Operation(summary = "查询语言配置详情")
    public Response languageSelectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(i18nService.languageSelectById(id));
    }

    @PostMapping("/language/save")
    @Operation(summary = "新增语言配置")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增语言配置:['+#p0+']'")
    public Response saveLanguage(@Validated({LanguageGroup.InsertGroup.class}) @RequestBody LanguageDTO data) {
        i18nService.saveLanguage(data);
        return Response.success();
    }

    @PostMapping("/language/update")
    @Operation(summary = "更新语言配置")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新语言配置:['+#p0+']'")
    public Response updateLanguage(@Validated({LanguageGroup.UpdatetGroup.class}) @RequestBody LanguageDTO data) {
        i18nService.updateLanguage(data);
        return Response.success();
    }

    @DeleteMapping("/language/delete")
    @Operation(summary = "删除语言配置")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除语言配置,语言ID集合:['+#p0+']'")
    public Response deleteLanguage(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        i18nService.deleteLanguage(ids);
        return Response.success();
    }

    @PostMapping("/message/pageList")
    @Operation(summary = "分页查询多语言消息")
    public Response messagePageList(@RequestBody MulitMessageQO qo) {
        return Response.success(i18nService.messagePageList(qo));
    }

    @GetMapping("/message/selectById/{id}")
    @Operation(summary = "查询多语言消息详情")
    public Response messageSelectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(i18nService.messageSelectById(id));
    }

    @GetMapping("/message/list/{messageKey}")
    @Operation(summary = "根据消息键查询翻译列表")
    public Response messageList(@NotBlank(message = "消息Key不能为空") @PathVariable String messageKey) {
        return Response.success(i18nService.messageListByKey(messageKey));
    }

    @PostMapping("/message/save")
    @Operation(summary = "新增多语言消息")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增多语言消息:['+#p0+']'")
    public Response saveMessage(@Validated({MultiMessageGroup.InsertGroup.class}) @RequestBody MultiMessageDTO data) {
        i18nService.saveMessage(data);
        return Response.success();
    }

    @PostMapping("/message/update")
    @Operation(summary = "更新多语言消息")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新多语言消息:['+#p0+']'")
    public Response updateMessage(@Validated({MultiMessageGroup.UpdatetGroup.class}) @RequestBody MultiMessageDTO data) {
        i18nService.updateMessage(data);
        return Response.success();
    }

    @DeleteMapping("/message/delete")
    @Operation(summary = "删除多语言消息")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除多语言消息,消息ID集合:['+#p0+']'")
    public Response deleteMessage(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        i18nService.deleteMessage(ids);
        return Response.success();
    }
}
