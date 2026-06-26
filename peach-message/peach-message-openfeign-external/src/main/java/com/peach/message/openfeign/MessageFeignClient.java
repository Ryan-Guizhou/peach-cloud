package com.peach.message.openfeign;

import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageRevokeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务Feign客户端
 */
@FeignClient(
        contextId = "messageFeignClient",
        name = ServiceNameConstant.MESSAGE_SERVICE,
        path = ServicePathConstant.MESSAGE_PATH_SERVICE
)
public interface MessageFeignClient {

    @PostMapping("/publish")
    Response publish(@RequestBody MessagePublishDTO data);

    @PostMapping("/publish/message")
    Response publishMessage(@RequestBody MessagePublishDTO data);

    @PostMapping("/publish/announcement")
    Response publishAnnouncement(@RequestBody MessagePublishDTO data);

    @PostMapping("/publish/todo")
    Response publishTodo(@RequestBody MessagePublishDTO data);

    @PostMapping("/revoke")
    Response revoke(@RequestBody MessageRevokeDTO data);

    @PostMapping("/revoke/message")
    Response revokeMessage(@RequestBody MessageRevokeDTO data);

    @PostMapping("/revoke/announcement")
    Response revokeAnnouncement(@RequestBody MessageRevokeDTO data);

    @PostMapping("/revoke/todo")
    Response revokeTodo(@RequestBody MessageRevokeDTO data);
}
