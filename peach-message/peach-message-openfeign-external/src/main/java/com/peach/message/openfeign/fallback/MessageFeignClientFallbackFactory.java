package com.peach.message.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageRevokeDTO;
import com.peach.message.openfeign.MessageFeignClient;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息 Feign 降级工厂。
 */
public class MessageFeignClientFallbackFactory implements FallbackFactory<MessageFeignClient> {

    private final PeachFeignFallbackSupport fallbackSupport;

    public MessageFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        this.fallbackSupport = fallbackSupport;
    }

    @Override
    public MessageFeignClient create(Throwable cause) {
        return new MessageFeignClient() {
            @Override
            public Response publish(MessagePublishDTO data) {
                return fail("publish", cause);
            }

            @Override
            public Response publishMessage(MessagePublishDTO data) {
                return fail("publishMessage", cause);
            }

            @Override
            public Response publishAnnouncement(MessagePublishDTO data) {
                return fail("publishAnnouncement", cause);
            }

            @Override
            public Response publishTodo(MessagePublishDTO data) {
                return fail("publishTodo", cause);
            }

            @Override
            public Response revoke(MessageRevokeDTO data) {
                return fail("revoke", cause);
            }

            @Override
            public Response revokeMessage(MessageRevokeDTO data) {
                return fail("revokeMessage", cause);
            }

            @Override
            public Response revokeAnnouncement(MessageRevokeDTO data) {
                return fail("revokeAnnouncement", cause);
            }

            @Override
            public Response revokeTodo(MessageRevokeDTO data) {
                return fail("revokeTodo", cause);
            }
        };
    }

    private Response fail(String method, Throwable cause) {
        return fallbackSupport.fail(ServiceContextConstant.MESSAGE_SERVICE_CONTEXT, method, cause);
    }
}
