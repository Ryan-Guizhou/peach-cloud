package com.peach.message.core.controller;

import com.peach.common.response.Response;
import com.peach.message.core.compont.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 消息推送接口
 */
@Slf4j
@RestController
@RequestMapping("/send")
public class MessageController {

    @Resource
    private WebSocketServer webSocketServer;

    /**
     * 推送给所有用户
     * @param msg 消息内容
     */
    @PostMapping("/broadcast")
    public Response broadcast(@RequestParam("msg") String msg) {
        webSocketServer.sendMessage(msg);
        return Response.success("Broadcast message sent");
    }

    /**
     * 推送给指定类型的指定用户
     * @param type 消息类型/业务类型
     * @param userId 用户ID
     * @param msg 消息内容
     */
    @PostMapping("/user")
    public Response sendToUser(@RequestParam("type") String type,
                               @RequestParam("userId") String userId,
                               @RequestParam("msg") String msg) {
        webSocketServer.sendMessage(msg, type, userId);
        return Response.success("Message sent to user: " + userId);
    }
    
    /**
     * 推送给指定类型的所有用户
     * @param type 消息类型/业务类型
     * @param msg 消息内容
     */
    @PostMapping("/type")
    public Response sendToType(@RequestParam("type") String type,
                               @RequestParam("msg") String msg) {
        webSocketServer.sendMessage(msg, type);
        return Response.success("Message sent to type: " + type);
    }
}
