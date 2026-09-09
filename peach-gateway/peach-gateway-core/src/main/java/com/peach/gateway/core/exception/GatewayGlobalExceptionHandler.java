package com.peach.gateway.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.SaTokenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.common.exception.BusinessException;
import com.peach.common.exception.LockException;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * 网关全局统一异常处理器
 * <p>
 * 捕获并拦截微服务网关层抛出的所有异常（包括路由失败、权限拦截、上游服务超时等），
 * 封装为统一的 {@link Response} 结构返回给前端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final byte[] FALLBACK_ERROR_BYTES =
            "{\"code\":\"500\",\"msg\":\"网关处理异常，请稍后重试\"}".getBytes(StandardCharsets.UTF_8);

    /**
     * 上游连接/超时相关的底座异常类型集合
     */
    private static final Set<Class<? extends Throwable>> UPSTREAM_UNAVAILABLE_EXCEPTIONS = Set.of(
            TimeoutException.class,
            SocketTimeoutException.class,
            ConnectException.class,
            UnknownHostException.class,
            ReadTimeoutException.class,
            io.netty.handler.timeout.TimeoutException.class,
            PrematureCloseException.class
    );

    private final ObjectMapper objectMapper;

    /**
     * 异常处理入口方法
     *
     * @param exchange  当前请求与响应上下文
     * @param exception 捕获到的 Throwable 实例
     * @return 异步 Void 结果，完成响应写出
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        // 1. 如果 Response 已经提交（已经开始输出到 Client），直接抛出，交给 WebFlux 默认机制处理
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        // 2. 解析异常，构建统一的返回体
        Response response = buildResponse(exception);
        byte[] body = writeBody(response);

        // 3. 动态确定响应状态码（如果是具体的 HTTP 标准错误，传递对应的 Status，否则默认 200）
        HttpStatusCode httpStatus = resolveHttpStatus(exception);
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 4. 写回 DataBuffer
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)
        ));
    }

    /**
     * 根据不同的异常类型提取错误码与提示信息，构建统一 Response 实体
     *
     * @param exception 捕获到的异常
     * @return 封装好的统一响应结果
     */
    private Response buildResponse(Throwable exception) {
        // 1. 业务自定义异常处理
        if (exception instanceof BusinessException businessException) {
            String code = StringUtil.isNotBlank(businessException.getCode())
                    ? businessException.getCode() : StatusEnum.BUSINESS_FAIL_CODE.getCode();
            String message = resolveMessage(businessException.getMsg(), businessException.getMessage(), "业务处理失败");
            log.warn("Gateway business exception handled, code={}, message={}", code, message);
            return Response.businessResponse(code, message);
        }

        // 2. 分布式锁/限流异常处理
        if (exception instanceof LockException lockException) {
            String message = resolveMessage(lockException.getMessage(), "请求过于频繁，请稍后再试");
            log.warn("Gateway lock exception handled, message={}", message);
            return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), message);
        }

        // 3. Sa-Token 权限与登录鉴权异常
        if (exception instanceof NotLoginException notLoginException) {
            String message = resolveMessage(notLoginException.getMessage(), "登录已失效，请重新登录");
            log.warn("Gateway not login exception handled, message={}", message);
            return Response.fail(StatusEnum.UNAUTHORIZED).setMsg(message);
        }
        if (exception instanceof NotPermissionException notPermissionException) {
            String message = resolveMessage(notPermissionException.getMessage(), "无权限访问");
            log.warn("Gateway not permission exception handled, message={}", message);
            return Response.fail(StatusEnum.FORBIDDEN).setMsg(message);
        }
        if (exception instanceof SaTokenException saTokenException) {
            String message = resolveMessage(saTokenException.getMessage(), "认证处理失败");
            log.warn("Gateway satoken exception handled, message={}", message);
            return Response.fail(StatusEnum.UNAUTHORIZED).setMsg(message);
        }

        // 4. Spring Cloud Gateway 路由找不到异常
        if (exception instanceof NotFoundException) {
            log.warn("Gateway route not found: {}", exception.getMessage());
            return Response.fail(StatusEnum.NOT_FOUND).setMsg("请求地址不存在");
        }

        // 5. Spring WebFlux 状态码异常处理
        if (exception instanceof ResponseStatusException responseStatusException) {
            return mapResponseStatusException(responseStatusException);
        }

        // 6. 检查是否为上游下游网络断开或超时异常
        if (isUpstreamUnavailable(exception)) {
            log.error("Gateway upstream service unavailable", exception);
            return Response.fail("服务暂不可用，请稍后重试");
        }

        // 7. 未知兜底系统异常
        log.error("Gateway system exception handled", exception);
        return Response.fail("网关处理异常，请稍后重试");
    }

    /**
     * 映射 WebFlux 原生的 ResponseStatusException 异常
     *
     * @param exception WebFlux 提供的响应状态码异常
     * @return 映射后的 Response 对象
     */
    private Response mapResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        String reason = exception.getReason();

        if (status == null) {
            log.error("Gateway response status exception handled with unknown status", exception);
            return Response.fail("网关处理异常，请稍后重试");
        }

        // 使用 Switch Pattern 替代繁重的 if-else 链，提升可读性
        return switch (status) {
            case BAD_REQUEST -> {
                log.warn("Gateway bad request handled, message={}", reason);
                yield Response.paramError(resolveMessage(reason, "请求参数错误"));
            }
            case UNAUTHORIZED -> {
                log.warn("Gateway unauthorized handled, message={}", reason);
                yield Response.fail(StatusEnum.UNAUTHORIZED).setMsg(resolveMessage(reason, "登录已失效，请重新登录"));
            }
            case FORBIDDEN -> {
                log.warn("Gateway forbidden handled, message={}", reason);
                yield Response.fail(StatusEnum.FORBIDDEN).setMsg(resolveMessage(reason, "无权限访问"));
            }
            case NOT_FOUND -> {
                log.warn("Gateway not found handled, message={}", reason);
                yield Response.fail(StatusEnum.NOT_FOUND).setMsg(resolveMessage(reason, "请求地址不存在"));
            }
            case CONFLICT -> {
                log.warn("Gateway conflict handled, message={}", reason);
                yield Response.businessResponse(String.valueOf(HttpStatus.CONFLICT.value()), resolveMessage(reason, "请求状态冲突，请确认后重试"));
            }
            case TOO_MANY_REQUESTS -> {
                log.warn("Gateway too many requests handled, message={}", reason);
                yield Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), resolveMessage(reason, "请求过于频繁，请稍后再试"));
            }
            case METHOD_NOT_ALLOWED -> {
                log.warn("Gateway method not allowed handled, message={}", reason);
                yield Response.fail(StatusEnum.NOT_SUPPORTED).setMsg(resolveMessage(reason, "请求方法不支持"));
            }
            case UNSUPPORTED_MEDIA_TYPE -> {
                log.warn("Gateway media type not supported handled, message={}", reason);
                yield Response.fail(StatusEnum.UNSUPPORTED_MEDIA_TYPE).setMsg(resolveMessage(reason, "不支持的媒体类型"));
            }
            default -> {
                if (status.is4xxClientError()) {
                    log.warn("Gateway client error handled, status={}, message={}", status.value(), reason);
                    yield Response.businessResponse(String.valueOf(status.value()), resolveMessage(reason, "请求处理失败"));
                }
                log.error("Gateway server error handled, status={}", status.value(), exception);
                yield Response.fail("服务暂不可用，请稍后重试");
            }
        };
    }

    /**
     * 判断 Cause 链条中是否包含上游网络连接或超时相关的底座异常
     *
     * @param exception 根异常
     * @return 若包含网络不可用/超时相关异常返回 true，否则返回 false
     */
    private boolean isUpstreamUnavailable(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            for (Class<? extends Throwable> clazz : UPSTREAM_UNAVAILABLE_EXCEPTIONS) {
                if (clazz.isInstance(current)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 针对部分原生标准异常，保留其真正的 HTTP 状态码（如 404/405/415 等），
     * 方便 API 监控工具与链路追踪系统正确收集 HTTP Status 指标。
     *
     * @param exception 捕获到的异常
     * @return 对应的 HttpStatusCode
     */
    private HttpStatusCode resolveHttpStatus(Throwable exception) {
        if (exception instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        return HttpStatus.OK;
    }

    /**
     * 将对象序列化为 JSON byte[]
     *
     * @param response 响应实体类
     * @return JSON 字节数组
     */
    private byte[] writeBody(Response response) {
        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException exception) {
            log.error("Gateway exception response serialization failed", exception);
            return FALLBACK_ERROR_BYTES;
        }
    }

    /**
     * 校验并提取优先使用的错误 Message 字符串
     *
     * @param primary        第一优先级消息
     * @param fallback       第二优先级消息
     * @param defaultMessage 默认兜底消息
     * @return 最终确定的消息字符串
     */
    private String resolveMessage(String primary, String fallback, String defaultMessage) {
        if (StringUtil.isNotBlank(primary)) {
            return primary;
        }
        if (StringUtil.isNotBlank(fallback)) {
            return fallback;
        }
        return defaultMessage;
    }

    /**
     * 校验并提取优先使用的错误 Message 字符串
     *
     * @param primary        第一优先级消息
     * @param defaultMessage 默认兜底消息
     * @return 最终确定的消息字符串
     */
    private String resolveMessage(String primary, String defaultMessage) {
        return resolveMessage(primary, null, defaultMessage);
    }
}
