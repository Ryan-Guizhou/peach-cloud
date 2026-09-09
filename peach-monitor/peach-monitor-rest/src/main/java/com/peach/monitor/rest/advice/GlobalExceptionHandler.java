package com.peach.monitor.rest.advice;

import com.peach.common.exception.BusinessException;
import com.peach.common.exception.LockException;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 监控服务统一异常处理。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.peach.monitor.rest")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Response handleBusinessException(BusinessException exception) {
        String code = StringUtil.isNotBlank(exception.getCode())
                ? exception.getCode() : StatusEnum.BUSINESS_FAIL_CODE.getCode();
        String message = resolveMessage(exception.getMsg(), exception.getMessage(), "业务处理失败");
        log.warn("Monitor business exception handled, code={}, message={}", code, message);
        return Response.businessResponse(code, message);
    }

    @ExceptionHandler(LockException.class)
    public Response handleLockException(LockException exception) {
        String message = resolveMessage(exception.getMessage(), "请求过于频繁，请稍后再试");
        log.warn("Monitor lock exception handled, message={}", message);
        return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), message);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Response handleBindException(Exception exception) {
        BindingResult bindingResult = exception instanceof MethodArgumentNotValidException methodArgumentNotValidException
                ? methodArgumentNotValidException.getBindingResult()
                : ((BindException) exception).getBindingResult();
        String message = resolveBindingMessage(bindingResult);
        log.warn("Monitor bind exception handled, message={}", message);
        return Response.paramError(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtil::isNotBlank)
                .findFirst()
                .orElse("参数错误");
        log.warn("Monitor constraint violation handled, message={}", message);
        return Response.paramError(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Response handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        String message = StringUtil.isNotBlank(exception.getName())
                ? exception.getName() + " 参数类型错误" : "参数类型错误";
        log.warn("Monitor argument type mismatch handled, message={}", message);
        return Response.paramError(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Response handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        String message = exception.getParameterName() + " 参数不能为空";
        log.warn("Monitor missing request parameter handled, message={}", message);
        return Response.paramError(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response handleIllegalArgumentException(IllegalArgumentException exception) {
        String message = resolveMessage(exception.getMessage(), "参数错误");
        log.warn("Monitor illegal argument handled, message={}", message);
        return Response.paramError(message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Response handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        log.warn("Monitor request method not supported, method={}", exception.getMethod());
        return Response.fail(StatusEnum.NOT_SUPPORTED).setMsg("请求方法不支持");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Response handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        log.warn("Monitor media type not supported, contentType={}", exception.getContentType());
        return Response.fail(StatusEnum.UNSUPPORTED_MEDIA_TYPE).setMsg("不支持的媒体类型");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Response handleNoHandlerFoundException(NoHandlerFoundException exception) {
        log.warn("Monitor request path not found, path={}", exception.getRequestURL());
        return Response.fail(StatusEnum.NOT_FOUND).setMsg("请求地址不存在");
    }

    @ExceptionHandler(Exception.class)
    public Response handleException(Exception exception) {
        if (isHandlerMethodValidationException(exception)) {
            log.warn("Monitor handler method validation exception handled");
            return Response.paramError("参数错误");
        }
        if (isDataConflictException(exception)) {
            log.warn("Monitor data conflict exception handled, type={}", exception.getClass().getName());
            return Response.businessResponse(StatusEnum.BUSINESS_FAIL_CODE.getCode(), "数据已存在或状态冲突");
        }
        log.error("Monitor system exception handled", exception);
        return Response.fail("系统异常，请稍后重试");
    }

    private String resolveBindingMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && StringUtil.isNotBlank(fieldError.getDefaultMessage())) {
            return fieldError.getDefaultMessage();
        }
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(StringUtil::isNotBlank)
                .findFirst()
                .orElse("参数错误");
    }

    private String resolveMessage(String primary, String fallback, String defaultMessage) {
        if (StringUtil.isNotBlank(primary)) {
            return primary;
        }
        if (StringUtil.isNotBlank(fallback)) {
            return fallback;
        }
        return defaultMessage;
    }

    private String resolveMessage(String primary, String defaultMessage) {
        return resolveMessage(primary, null, defaultMessage);
    }

    private boolean isHandlerMethodValidationException(Exception exception) {
        return "org.springframework.web.method.annotation.HandlerMethodValidationException"
                .equals(exception.getClass().getName());
    }

    private boolean isDataConflictException(Exception exception) {
        String className = exception.getClass().getName();
        return "org.springframework.dao.DuplicateKeyException".equals(className)
                || "org.springframework.dao.DataIntegrityViolationException".equals(className);
    }
}
