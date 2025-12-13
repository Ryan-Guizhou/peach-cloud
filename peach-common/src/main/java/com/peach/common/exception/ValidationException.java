package com.peach.common.exception;



import com.peach.common.validate.CommonValidator;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description 校验异常
 * @CreateTime 04 3月 2025 21:30
 */
public class ValidationException extends RuntimeException{

    private final Set<CommonValidator.ValidationDetail> fieldErrors;

    // 保留一个主要构造方法，避免冲突
    public ValidationException(String message, Set<CommonValidator.ValidationDetail> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashSet<>(fieldErrors) : Collections.emptySet();
    }

    // 从ConstraintViolation创建
    public ValidationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(buildMessage(constraintViolations));
        this.fieldErrors = constraintViolations.stream()
                .map(v -> new CommonValidator.ValidationDetail(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .collect(Collectors.toSet());
    }

    // 简单错误消息
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Collections.emptySet();
    }

    private static String buildMessage(Set<? extends ConstraintViolation<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return "参数校验失败";
        }
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

}
