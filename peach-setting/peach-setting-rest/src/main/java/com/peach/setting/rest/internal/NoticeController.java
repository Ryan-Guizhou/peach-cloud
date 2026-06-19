package com.peach.setting.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.common.response.Response;
import com.peach.setting.comon.enums.NoticeGroup;
import com.peach.setting.dto.NoticeDTO;
import com.peach.setting.dto.NoticePublishDTO;
import com.peach.setting.qo.NoticeQO;
import com.peach.setting.qo.SiteMessageQO;
import com.peach.setting.service.INoticeService;
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
 * @Description 公告与站内信接口
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/setting/notice")
@Tag(name = "公告与站内信接口", description = "公告与站内信接口")
public class NoticeController {

    @Resource
    private INoticeService noticeService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询公告")
    public Response pageList(@RequestBody NoticeQO qo) {
        return Response.success(noticeService.noticePageList(qo));
    }

    @GetMapping("/selectById/{id}")
    @Operation(summary = "查询公告详情")
    public Response selectById(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        return Response.success(noticeService.noticeSelectById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "新增公告")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增公告:['+#p0+']'")
    public Response save(@Validated({NoticeGroup.InsertGroup.class}) @RequestBody NoticeDTO data) {
        noticeService.saveNotice(data);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新公告")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'更新公告:['+#p0+']'")
    public Response update(@Validated({NoticeGroup.UpdatetGroup.class}) @RequestBody NoticeDTO data) {
        noticeService.updateNotice(data);
        return Response.success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除公告")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除公告,公告ID集合:['+#p0+']'")
    public Response delete(@NotNull(message = "ID集合不能为空") @RequestBody List<String> ids) {
        noticeService.deleteNotice(ids);
        return Response.success();
    }

    @PostMapping("/publish")
    @Operation(summary = "发布公告")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'发布公告:['+#p0+']'")
    public Response publish(@Validated({NoticeGroup.PublishGroup.class}) @RequestBody NoticePublishDTO data) {
        noticeService.publishNotice(data);
        return Response.success();
    }

    @PostMapping("/revoke/{id}")
    @Operation(summary = "撤销公告")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.WARN, optContent = "'撤销公告,公告ID:['+#p0+']'")
    public Response revoke(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        noticeService.revokeNotice(id);
        return Response.success();
    }

    @PostMapping("/read/{noticeCode}/{userId}")
    @Operation(summary = "标记公告已读")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'标记公告已读,公告编码:['+#p0+'],用户ID:['+#p1+']'")
    public Response read(@NotBlank(message = "公告编码不能为空") @PathVariable String noticeCode,
                         @NotBlank(message = "用户ID不能为空") @PathVariable String userId) {
        noticeService.markNoticeRead(noticeCode, userId);
        return Response.success();
    }

    @PostMapping("/message/pageList")
    @Operation(summary = "分页查询站内信")
    public Response messagePageList(@RequestBody SiteMessageQO qo) {
        return Response.success(noticeService.siteMessagePageList(qo));
    }

    @PostMapping("/message/read/{id}")
    @Operation(summary = "标记站内信已读")
    @UserOperLog(moduleCode = UserLogEnum.Module.SETTING, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'标记站内信已读,站内信ID:['+#p0+']'")
    public Response messageRead(@NotBlank(message = "主键ID不能为空") @PathVariable String id) {
        noticeService.markSiteMessageRead(id);
        return Response.success();
    }
}
