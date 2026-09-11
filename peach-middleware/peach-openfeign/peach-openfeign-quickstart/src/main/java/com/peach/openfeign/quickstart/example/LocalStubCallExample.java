package com.peach.openfeign.quickstart.example;

import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import com.peach.openfeign.quickstart.client.LocalStubClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 通过 Feign 调用本机 Stub：成功回显，以及 ErrorDecoder 对 500/408 的异常分类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class LocalStubCallExample {

    private final LocalStubClient localStubClient;

    /**
     * 成功调用本机 Stub。
     *
     * @param message 回显内容
     * @return Stub 响应
     */
    public Map<String, String> echo(String message) {
        Map<String, String> body = localStubClient.echo(message);
        log.info("feign echo ok, source={}, message={}", body.get("source"), body.get("message"));
        return body;
    }

    /**
     * 调用 Stub 500，期望得到 {@link PeachFeignRemoteException}。
     *
     * @return 分类后的远端异常
     */
    public PeachFeignRemoteException failAsRemoteError() {
        try {
            localStubClient.fail();
        } catch (PeachFeignRemoteException exception) {
            log.info("feign fail classified as remote error, status={}, client={}",
                    exception.getStatus(), exception.getClientName());
            return exception;
        }
        throw new IllegalStateException("expected PeachFeignRemoteException");
    }

    /**
     * 调用 Stub 408，期望得到 {@link PeachFeignTimeoutException}。
     *
     * @return 分类后的超时异常
     */
    public PeachFeignTimeoutException timeoutAsClassified() {
        try {
            localStubClient.timeout();
        } catch (PeachFeignTimeoutException exception) {
            log.info("feign timeout classified, client={}, method={}",
                    exception.getClientName(), exception.getMethodKey());
            return exception;
        }
        throw new IllegalStateException("expected PeachFeignTimeoutException");
    }
}
