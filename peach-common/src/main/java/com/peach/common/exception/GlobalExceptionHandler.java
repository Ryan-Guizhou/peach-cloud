package com.peach.common.exception;

import com.peach.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:42
 * @Description 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler{

    @ExceptionHandler(BusinessException.class)
    public Response handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        return Response.businessResponse(e.getMsg());
    }

    @ExceptionHandler(LockException.class)
    public Response handleLockException(LockException e) {
        log.error("LockException: {}", e.getMessage(), e);
        return Response.businessResponse(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Response handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage(), e);
        return Response.businessResponse(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return Response.fail();
    }

}
