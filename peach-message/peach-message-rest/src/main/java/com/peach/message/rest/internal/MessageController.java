package com.peach.message.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.common.response.Response;

import com.peach.message.common.MessageCategoryConfig;
import com.peach.message.common.enums.MessageEnum;
import com.peach.message.dto.MessageReadDTO;
import com.peach.message.qo.SiteMessageQO;
import com.peach.message.service.IMessageService;
import com.peach.satoken.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 站内信接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 站内信接口
 */
@Indexed
@Validated
@RestController
@RequestMapping("/message")
@Tag(name = "站内信接口", description = "站内信接口")
@RequiredArgsConstructor
public class MessageController {

    private static final String USER_NOT_LOGGED_IN_MESSAGE = "当前用户未登录";

    private final IMessageService messageService;

    @GetMapping("/query")
    @Operation(summary = "查询站内信列表")
    public Response query(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                          @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                          @RequestParam(value = "readFlag", required = false) Integer readFlag) {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        SiteMessageQO qo = new SiteMessageQO();
        qo.setPageNum(pageNum);
        qo.setPageSize(pageSize);
        qo.setReceiverId(userId);
        qo.setReadFlag(readFlag);
        return messageService.pageList(qo);
    }

    @GetMapping("/message")
    @Operation(summary = "查询消息列表")
    public Response queryMessage(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                 @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                                 @RequestParam(value = "readFlag", required = false) Integer readFlag,
                                 @RequestParam(value = "messageType", required = false) String messageType) {
        return queryByCategory(MessageEnum.MessageCategory.MESSAGE.getCode(), messageType, pageNum, pageSize, readFlag);
    }

    @GetMapping("/announcement")
    @Operation(summary = "查询公告列表")
    public Response queryAnnouncement(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                                      @RequestParam(value = "readFlag", required = false) Integer readFlag,
                                      @RequestParam(value = "messageType", required = false) String messageType) {
        return queryByCategory(MessageEnum.MessageCategory.ANNOUNCEMENT.getCode(), messageType, pageNum, pageSize, readFlag);
    }

    @GetMapping("/todo")
    @Operation(summary = "查询待办列表")
    public Response queryTodo(@RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                              @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                              @RequestParam(value = "readFlag", required = false) Integer readFlag,
                              @RequestParam(value = "messageType", required = false) String messageType) {
        return queryByCategory(MessageEnum.MessageCategory.TODO.getCode(), messageType, pageNum, pageSize, readFlag);
    }

    @PostMapping("/pageList")
    @Operation(summary = "分页查询站内信")
    public Response queryPage(@RequestBody SiteMessageQO qo) {
        return messageService.pageList(qo);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "查询未读数")
    public Response queryUnreadCount() {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        return messageService.unreadCount(userId);
    }

    @GetMapping("/message/unread-count")
    @Operation(summary = "查询消息未读数")
    public Response queryMessageUnreadCount(@RequestParam(value = "messageType", required = false) String messageType) {
        return queryUnreadCountByCategory(MessageEnum.MessageCategory.MESSAGE.getCode(), messageType);
    }

    @GetMapping("/announcement/unread-count")
    @Operation(summary = "查询公告未读数")
    public Response queryAnnouncementUnreadCount(@RequestParam(value = "messageType", required = false) String messageType) {
        return queryUnreadCountByCategory(MessageEnum.MessageCategory.ANNOUNCEMENT.getCode(), messageType);
    }

    @GetMapping("/todo/unread-count")
    @Operation(summary = "查询待办未读数")
    public Response queryTodoUnreadCount(@RequestParam(value = "messageType", required = false) String messageType) {
        return queryUnreadCountByCategory(MessageEnum.MessageCategory.TODO.getCode(), messageType);
    }

    @PostMapping("/{messageId}/read")
    @Operation(summary = "标记站内信已读")
    public Response read(@NotBlank(message = "消息ID不能为空") @PathVariable String messageId) {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        MessageReadDTO data = new MessageReadDTO();
        data.setMessageId(messageId);
        data.setReceiverId(userId);
        return messageService.read(data);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Response readAll() {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        return messageService.readAll(userId);
    }

    @PostMapping("/message/read-all")
    @Operation(summary = "全部消息标记已读")
    public Response readAllMessage(@RequestParam(value = "messageType", required = false) String messageType) {
        return readAllByCategory(MessageEnum.MessageCategory.MESSAGE.getCode(), messageType);
    }

    @PostMapping("/announcement/read-all")
    @Operation(summary = "全部公告标记已读")
    public Response readAllAnnouncement(@RequestParam(value = "messageType", required = false) String messageType) {
        return readAllByCategory(MessageEnum.MessageCategory.ANNOUNCEMENT.getCode(), messageType);
    }

    @PostMapping("/todo/read-all")
    @Operation(summary = "全部待办标记已读")
    public Response readAllTodo(@RequestParam(value = "messageType", required = false) String messageType) {
        return readAllByCategory(MessageEnum.MessageCategory.TODO.getCode(), messageType);
    }

    private Response queryByCategory(String messageCategory, String messageType, Integer pageNum, Integer pageSize, Integer readFlag) {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        SiteMessageQO qo = new SiteMessageQO();
        qo.setPageNum(pageNum);
        qo.setPageSize(pageSize);
        qo.setReceiverId(userId);
        qo.setReadFlag(readFlag);
        qo.setMessageTypeList(MessageCategoryConfig.getTypes(MessageEnum.MessageCategory.valueOf(messageCategory)));
        qo.setMessageType(messageType);
        return messageService.pageList(qo);
    }

    private Response queryUnreadCountByCategory(String messageCategory, String messageType) {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        return messageService.unreadCount(userId, messageCategory, messageType);
    }

    private Response readAllByCategory(String messageCategory, String messageType) {
        String userId = SecurityContextHolder.currentUserId();
        if (StringUtils.isBlank(userId)) {
            return Response.businessResponse(USER_NOT_LOGGED_IN_MESSAGE);
        }
        return messageService.readAll(userId, messageCategory, messageType);
    }


}
