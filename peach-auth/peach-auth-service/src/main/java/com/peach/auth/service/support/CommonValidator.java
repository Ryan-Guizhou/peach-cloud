package com.peach.auth.service.support;

import com.peach.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class CommonValidator {

    private final Validator validator;

    public void validate(Object target) {
        validate(target, Default.class);
    }

    public void validate(Object target, Class<?> group) {
        validate(target, new Class<?>[]{group});
    }

    public void validate(Object target, Class<?>... groups) {
        Optional.ofNullable(target)
                .ifPresent(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);

                    if (!violations.isEmpty()) {
                        Set<String> messages = convertToMessage(violations);
                        String errorMessage = messages.stream().collect(Collectors.joining(","));
                        log.error("Object validation failed with {} error(s): {}", violations.size(), errorMessage);
                        throw new ValidationException(errorMessage);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Object validation passed: {}", validObj.getClass().getSimpleName());
                    }
                });
    }

    public Set<String> getValidationMessages(Object target) {
        return getValidationMessages(target, Default.class);
    }

    public Set<String> getValidationMessages(Object target, Class<?> group) {
        return getValidationMessages(target, new Class<?>[]{group});
    }

    public Set<String> getValidationMessages(Object target, Class<?>... groups) {
        return Optional.ofNullable(target)
                .map(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
                    return convertToMessage(violations);
                })
                .orElse(Set.of("校验对象不能为空"));
    }

    public Set<ValidationException.ValidationDetail> getValidationErrors(Object target) {
        return getValidationErrors(target, Default.class);
    }

    public Set<ValidationException.ValidationDetail> getValidationErrors(Object target, Class<?> group) {
        return getValidationErrors(target, new Class<?>[]{group});
    }

    public Set<ValidationException.ValidationDetail> getValidationErrors(Object target, Class<?>... groups) {
        return Optional.ofNullable(target)
                .map(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
                    return violations.stream()
                            .map(v -> new ValidationException.ValidationDetail(v.getPropertyPath().toString(), v.getMessage()))
                            .collect(Collectors.toUnmodifiableSet());
                })
                .orElse(Set.of(new ValidationException.ValidationDetail("", "校验对象不能为空")));
    }

    public boolean isValid(Object target) {
        return isValid(target, Default.class);
    }

    public boolean isValid(Object target, Class<?> group) {
        return isValid(target, new Class<?>[]{group});
    }

    public boolean isValid(Object target, Class<?>... groups) {
        return Optional.ofNullable(target)
                .map(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
                    return violations.isEmpty();
                })
                .orElse(false);
    }

    public void validateProperty(Object target, String propertyName) {
        validateProperty(target, propertyName, Default.class);
    }

    public void validateProperty(Object target, String propertyName, Class<?> group) {
        validateProperty(target, propertyName, new Class<?>[]{group});
    }

    public void validateProperty(Object target, String propertyName, Class<?>... groups) {
        Optional.ofNullable(target)
                .ifPresent(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations =
                            validator.validateProperty(validObj, propertyName, validationGroups);
                    if (!violations.isEmpty()) {
                        Set<String> messages = convertToMessage(violations);
                        String errorMessage = messages.stream().collect(Collectors.joining(","));
                        log.error("Object validation failed with {} error(s): {}", violations.size(), errorMessage);
                        throw new ValidationException(errorMessage);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Object validation passed: {}", validObj.getClass().getSimpleName());
                    }
                });
    }

    public void validateAll(Object... objects) {
        validateAll(new Class<?>[]{Default.class}, objects);
    }

    public void validateAll(Class<?> group, Object... objects) {
        validateAll(new Class<?>[]{group}, objects);
    }

    public void validateAll(Class<?>[] groups, Object... objects) {
        Optional.ofNullable(objects)
                .filter(arr -> arr.length > 0)
                .ifPresent(validObjects -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    collectAndThrowErrors(validObjects, validationGroups);
                });
    }

    private void collectAndThrowErrors(Object[] objects, Class<?>[] groups) {
        Set<ValidationException.ValidationDetail> errors = Arrays.stream(objects)
                .filter(Objects::nonNull)
                .map(target -> getValidationErrors(target, groups))
                .flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());

        createErrorMessage(errors).ifPresent(message -> {
            log.error(message);
            throw new ValidationException(message, errors);
        });
    }

    private Class<?>[] resolveValidationGroups(Class<?>[] groups) {
        return Optional.ofNullable(groups)
                .filter(arr -> arr.length > 0)
                .orElse(new Class<?>[]{Default.class});
    }

    private Optional<String> createErrorMessage(Set<ValidationException.ValidationDetail> errors) {
        return Optional.of(errors)
                .filter(e -> !e.isEmpty())
                .map(e -> e.stream()
                        .map(ValidationException.ValidationDetail::getMessage)
                        .collect(Collectors.joining("; ")));
    }

    private Set<String> convertToMessage(Set<ConstraintViolation<Object>> violations) {
        return violations.stream()
                .map(v -> {
                    String field = v.getPropertyPath().toString();
                    return isNotBlank(field) ? field + ": " + v.getMessage() : v.getMessage();
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }
}
