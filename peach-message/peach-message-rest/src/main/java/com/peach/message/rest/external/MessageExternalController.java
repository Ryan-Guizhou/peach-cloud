package com.peach.message.rest.external;

import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageRevokeDTO;
import com.peach.message.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务外部接口
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/message/external")
@Tag(name = "消息服务外部接口", description = "消息服务外部接口")
public class MessageExternalController {

    @Resource
    private IMessageService messageService;

    @PostMapping("/publish")
    @Operation(summary = "发布消息")
    public Response publish(@Validated @RequestBody MessagePublishDTO data) {
        return messageService.publish(data);
    }

    @PostMapping("/publish/message")
    @Operation(summary = "发布消息")
    public Response publishMessage(@Validated @RequestBody MessagePublishDTO data) {
        return messageService.publishMessage(data);
    }

    @PostMapping("/publish/announcement")
    @Operation(summary = "发布公告")
    public Response publishAnnouncement(@Validated @RequestBody MessagePublishDTO data) {
        return messageService.publishAnnouncement(data);
    }

    @PostMapping("/publish/todo")
    @Operation(summary = "发布待办")
    public Response publishTodo(@Validated @RequestBody MessagePublishDTO data) {
        return messageService.publishTodo(data);
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤销消息")
    public Response revoke(@Validated @RequestBody MessageRevokeDTO data) {
        return messageService.revoke(data);
    }

    @PostMapping("/revoke/message")
    @Operation(summary = "撤销消息")
    public Response revokeMessage(@Validated @RequestBody MessageRevokeDTO data) {
        return messageService.revokeMessage(data);
    }

    @PostMapping("/revoke/announcement")
    @Operation(summary = "撤销公告")
    public Response revokeAnnouncement(@Validated @RequestBody MessageRevokeDTO data) {
        return messageService.revokeAnnouncement(data);
    }

    @PostMapping("/revoke/todo")
    @Operation(summary = "撤销待办")
    public Response revokeTodo(@Validated @RequestBody MessageRevokeDTO data) {
        return messageService.revokeTodo(data);
    }
}
