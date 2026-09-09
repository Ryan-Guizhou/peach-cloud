package com.peach.common.exception;
import jakarta.validation.ConstraintViolation;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 校验异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 04 3月 2025 21:30
 * @Description 校验异常
 */
public class ValidationException extends RuntimeException{

    private final transient Set<ValidationDetail> fieldErrors;


    public ValidationException(String message, Set<ValidationDetail> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashSet<>(fieldErrors) : Set.of();
    }

    public Set<ValidationDetail> getFieldErrors() {
        return fieldErrors;
    }


    public ValidationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(buildMessage(constraintViolations));
        this.fieldErrors = constraintViolations.stream()
                .map(v -> new ValidationDetail(
                        v.getPropertyPath().toString(),
                        v.getMessage()
                ))
                .collect(Collectors.toUnmodifiableSet());
    }


    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Set.of();
    }

    private static String buildMessage(Set<? extends ConstraintViolation<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return "参数校验失败";
        }
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    public static final class ValidationDetail {

        private final String field;

        private final String message;

        public ValidationDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
