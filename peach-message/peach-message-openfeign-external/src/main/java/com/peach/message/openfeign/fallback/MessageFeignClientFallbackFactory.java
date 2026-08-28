package com.peach.message.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageRevokeDTO;
import com.peach.message.openfeign.MessageFeignClient;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 消息 Feign 降级工厂。
 *
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
        return new FallbackMessageFeignClient(fallbackSupport, cause);
    }

    /**
     * Fallback消息Feign客户端。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class FallbackMessageFeignClient implements MessageFeignClient {

        private final PeachFeignFallbackSupport fallbackSupport;
        private final Throwable cause;

        private FallbackMessageFeignClient(PeachFeignFallbackSupport fallbackSupport, Throwable cause) {
            this.fallbackSupport = fallbackSupport;
            this.cause = cause;
        }

        @Override
        public Response publish(MessagePublishDTO data) {
            return fail("publish");
        }

        @Override
        public Response publishMessage(MessagePublishDTO data) {
            return fail("publishMessage");
        }

        @Override
        public Response publishAnnouncement(MessagePublishDTO data) {
            return fail("publishAnnouncement");
        }

        @Override
        public Response publishTodo(MessagePublishDTO data) {
            return fail("publishTodo");
        }

        @Override
        public Response revoke(MessageRevokeDTO data) {
            return fail("revoke");
        }

        @Override
        public Response revokeMessage(MessageRevokeDTO data) {
            return fail("revokeMessage");
        }

        @Override
        public Response revokeAnnouncement(MessageRevokeDTO data) {
            return fail("revokeAnnouncement");
        }

        @Override
        public Response revokeTodo(MessageRevokeDTO data) {
            return fail("revokeTodo");
        }

        private Response fail(String method) {
            return fallbackSupport.fail(ServiceContextConstant.MESSAGE_SERVICE_CONTEXT, method, cause);
        }
    }
}
