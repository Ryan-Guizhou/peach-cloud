package com.peach.common.validate;

import com.peach.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 通用验证器工具类
 * 提供对象属性验证、批量验证等功能
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/11/10 18:34
 */
@Slf4j
@Component
public class CommonValidator {

    private final Validator validator;


    public CommonValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * 校验对象（使用默认分组）
     * @param target 被检验的对象
     */
    public void validate(Object target) {
        validate(target, new Class<?>[]{Default.class});
    }

    /**
     * 校验对象（指定单个分组）
     * @param target 被检验的对象
     * @param group 分组
     */
    public void validate(Object target, Class<?> group) {
        validate(target, new Class<?>[]{group});
    }

    /**
     *  校验对象（支持多个分组）
     * @param target 被检验的对象
     * @param groups 分组 数组
     */

    public void validate(Object target, Class<?>... groups) {
        Optional.ofNullable(target)
                .ifPresent(validObj -> {
                            Class<?>[] validationGroups = resolveValidationGroups(groups);
                            Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);

                            if (!violations.isEmpty()) {
                                Set<String> set = convertToMessage(violations);
                                String errorMessage = set.stream().collect(Collectors.joining(","));
                                log.error("对象验证失败，共{}个错误：{}", violations.size(), errorMessage);
                                throw new ValidationException(errorMessage);
                            }

                            if (log.isDebugEnabled()) {
                                log.debug("对象验证通过: {}", validObj.getClass().getSimpleName());
                            }
                        }
                );
    }

    /**
     * 校验并返回所有错误信息（不抛异常）- 默认分组
     * @param target 被检验的对象
     * @return 错误信息集合
     */
    public Set<String> getValidationMessages(Object target) {
        return getValidationMessages(target, new Class<?>[]{Default.class});
    }

    /**
     * 校验并返回所有错误信息（不抛异常）- 单个分组
     * @param target 被检验的对象
     * @param group 分组
     * @return 错误信息集合
     */
    public Set<String> getValidationMessages(Object target, Class<?> group) {
        return getValidationMessages(target, new Class<?>[]{group});
    }

    /**
     * 校验并返回所有错误信息（不抛异常）- 多个分组
     * @param target 被检验的对象
     * @param groups 分组 数组
     * @return 错误信息集合
     */
    public Set<String> getValidationMessages(Object target, Class<?>... groups) {
        return Optional.ofNullable(target)
                .map(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);

                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
                    return convertToMessage(violations);
                })
                .orElse(Collections.singleton("校验对象不能为null"));
    }

    /**
     * 校验并返回详细错误信息（包含字段路径）- 默认分组
     * @param target 被检验的对象
     * @return 详细错误信息集合
     */
    public Set<ValidationDetail> getValidationErrors(Object target) {
        return getValidationErrors(target, new Class<?>[]{Default.class});
    }

    /**
     * 校验并返回详细错误信息（包含字段路径）- 单个分组
     * @param target 被检验的对象
     * @param group 分组
     * @return 详细错误信息集合
     */
    public Set<ValidationDetail> getValidationErrors(Object target, Class<?> group) {
        return getValidationErrors(target, new Class<?>[]{group});
    }

    /**
     * 校验并返回详细错误信息（包含字段路径）- 多个分组
     * @param target 被检验的对象
     * @param groups 分组 数组
     * @return 详细错误信息集合
     */

    public Set<ValidationDetail> getValidationErrors(Object target, Class<?>... groups) {
        return Optional.ofNullable( target)
                .map(validObj -> {
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
                    return violations.stream()
                            .map(v -> new ValidationDetail(v.getPropertyPath().toString(), v.getMessage()))
                            .collect(Collectors.toSet());
                }).orElse(Collections.singleton(new ValidationDetail("", "校验对象不能为null")));
    }

    /**
     * 快速校验 - 只返回是否通过（默认分组）
     * @param target 被检验的对象
     * @return 是否有效
     */
    public boolean isValid(Object target) {
        return isValid(target, new Class<?>[]{Default.class});
    }

    /**
     * 快速校验 - 只返回是否通过（单个分组）
     * @param target 被检验的对象
     * @param group 分组
     * @return 是否有效
     */
    public boolean isValid(Object target, Class<?> group) {
        return isValid(target, new Class<?>[]{group});
    }

    /**
     * 快速校验 - 只返回是否通过（多个分组）
     * @param target 被检验的对象
     * @param groups 分组 数组
     * @return 是否有效
     */

    public boolean isValid(Object target, Class<?>... groups) {
        return Optional.ofNullable(target).map(validObj -> {
            Class<?>[] validationGroups = resolveValidationGroups(groups);
            Set<ConstraintViolation<Object>> violations = validator.validate(validObj, validationGroups);
            return violations.isEmpty();
        }).orElse(false);
    }

    /**
     * 校验对象的特定属性
     * @param target 被检验的对象
     * @param propertyName 属性名称
     */
    public void validateProperty(Object target, String propertyName) {
        validateProperty(target, propertyName, new Class<?>[]{Default.class});
    }

    /**
     * 校验对象的特定属性 - 单个分组
     * @param target 被检验的对象
     * @param propertyName 属性名称
     * @param group 分组
     */
    public void validateProperty(Object target, String propertyName, Class<?> group) {
        validateProperty(target, propertyName, new Class<?>[]{group});
    }

    /**
     * 校验对象的特定属性 - 多个分组
     * @param target 被检验的对象
     * @param propertyName 属性名称
     * @param groups 分组 数组
     */
    public void validateProperty(Object target, String propertyName, Class<?>... groups) {

        Optional.ofNullable(target)
                .ifPresent(validObj ->{
                    Class<?>[] validationGroups = resolveValidationGroups(groups);
                    Set<ConstraintViolation<Object>> violations = validator.validateProperty(target, propertyName, validationGroups);
                    if (!violations.isEmpty()) {
                        Set<String> set = convertToMessage(violations);
                        String errorMessage = set.stream().collect(Collectors.joining(","));
                        log.error("对象验证失败，共{}个错误：{}", violations.size(), errorMessage);
                        throw new ValidationException(errorMessage);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("对象验证通过: {}", validObj.getClass().getSimpleName());
                    }
                });

    }

    /**
     * 批量校验多个对象
     * @param objects 被校验的对象数组
     */
    public void validateAll(Object... objects) {
        validateAll(new Class<?>[]{Default.class}, objects);
    }

    /**
     * 批量校验多个对象 - 单个分组
     * @param group 分组
     * @param objects 被校验的对象数组
     */
    public void validateAll(Class<?> group, Object... objects) {
        validateAll(new Class<?>[]{group}, objects);
    }

    /**
     * 批量校验多个对象 - 多个分组
     * @param groups 分组 数组
     * @param objects 被校验的对象数组
     */
    public void validateAll(Class<?>[] groups, Object... objects) {
        // 处理空对象数组
        Optional.ofNullable(objects)
                .filter(arr -> arr.length > 0)
                .ifPresent(validObjects -> {
                    // 确定验证组
                    Class<?>[] validationGroups = resolveValidationGroups(groups);

                    // 收集所有错误
                    collectAndThrowErrors(validObjects, validationGroups);
                });

    }

    /**
     * 批量校验多个对象
     * @param objects 被校验的对象数组
     * @param groups 验证组
     */
    private void collectAndThrowErrors(Object[] objects, Class<?>[] groups) {
        Set<ValidationDetail> errors = Arrays.stream(objects)
                .filter(Objects::nonNull)
                .map(target -> getValidationErrors(target, groups))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // 如果有错误则抛出异常
        createErrorMessage(errors).ifPresent(message -> {
            log.error(message);
            throw new ValidationException(message);
        });
    }

    /**
     * 确定验证组
     * @param groups 分组
     * @return 验证组
     */
    private Class<?>[] resolveValidationGroups(Class<?>[] groups) {
        return Optional.ofNullable(groups)
                .filter(arr -> arr.length > 0)
                .orElse(new Class<?>[]{Default.class});
    }

    /**
     * 创建错误信息
     * @param errors 错误信息集合
     * @return 错误信息
     */
    private Optional<String> createErrorMessage(Set<ValidationDetail> errors) {
        return Optional.of(errors)
                .filter(e -> !e.isEmpty())
                .map(e -> e.stream()
                        .map(ValidationDetail::getMessage)
                        .collect(Collectors.joining("; ")));
    }


    /**
     * 转换错误信息
     * @param violations 错误信息
     * @return 错误信息
     */
    private Set<String> convertToMessage(Set<ConstraintViolation<Object>> violations) {
        return violations.stream()
                .map(v -> {
                    String field = v.getPropertyPath().toString();
                    return isNotBlank(field) ? field + ": " + v.getMessage() : v.getMessage();
                }).collect(Collectors.toSet());
    }

    /**
     * 判断字符串是否非空
     * @param str 字符串
     * @return 是否非空
     */
    private static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     *  字段错误信息
     */
    public static class ValidationDetail {

        private String field;

        private String message;

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

        @Override
        public String toString() {
            return isNotBlank(field) ? field + ": " + message : message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o){
                return true;
            }
            if (o == null || getClass() != o.getClass()){
                return false;
            }
            ValidationDetail that = (ValidationDetail) o;
            return Objects.equals(field, that.field) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, message);
        }
    }
}

