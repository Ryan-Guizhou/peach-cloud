package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.openfeign.MessageFeignClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Arrays;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/2 11:42
 */
@Indexed
@RestController
@RequestMapping("/auth/demo")
@Tag(name = "DemoMessage", description = "资源管理")
@ConditionalOnProperty(prefix = "peach.demo", name = "feign-enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoMessage {

        private final MessageFeignClient messageFeignClient;

    @PostMapping("")
    public Response upload() {
        MessagePublishDTO messagePublishDTO = new MessagePublishDTO();
        messagePublishDTO.setMessageType("ceshi");
        messagePublishDTO.setReceiverIds(Arrays.asList("1","2","3"));
        return messageFeignClient.publish(messagePublishDTO);
    }

}
