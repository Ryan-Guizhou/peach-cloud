package com.peach.common;

import java.io.Serial;

import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.ObjectUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Peach数据对象。
 * <p>
 * 该类主要用于承载所有数据库实体通用的审计字段和基础工具方法。
 * 所有 DO / Entity 实体可以继承该类，统一获得创建人、创建时间、更新人、更新时间等基础字段。
 * </p>
 * <p>
 * 注意：
 * <ul>
 * <li>该类不建议承担 DTO、VO 转换职责，DTO/DO/VO 转换建议交给 Converter / Assembler。</li>
 * <li>该类不建议包含具体业务默认值逻辑，业务默认值应由子类或业务层处理。</li>
 * <li>该类提供的反射方法主要用于通用框架能力，业务代码中应谨慎使用。</li>
 * </ul>
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 */
@Data
public class PeachDO implements Serializable {

    private static final String CURRENT_USER_ID_FIELD = "currentUserId";


    @Serial
    private static final long serialVersionUID = -3930151180455626026L;

    /**
     * 缓存每个实体类上的主键字段，避免重复反射扫描。
     */
    private static final Map<Class<?>, Field> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    private static final String SECURITY_CONTEXT_HOLDER_CLASS = "com.peach.satoken.context.SecurityContextHolder";

    private static final String TENANT_ID_FIELD = "tenantId";

    private static final String ORG_ID_FIELD = "orgId";

    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private String createdTime;

    @Column(name = "CREATOR_ID")
    @Schema(description = "创建人ID")
    private String creatorId;

    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private String modifyTime;

    @Column(name = "MODIFIER_ID")
    @Schema(description = "修改人ID")
    private String modifierId;

    /**
     * 根据 Map 创建指定类型的实体对象。
     * <p>
     * Map 中的 key 需要与目标对象的属性名保持一致。
     * 该方法适用于简单对象属性拷贝，不建议用于复杂嵌套对象转换。
     * </p>
     *
     * @param clazz 目标对象类型，不能为 null
     * @param map   属性 Map，可为 null
     * @param <E>   目标对象泛型
     * @return 创建并填充属性后的目标对象
     * @throws IllegalArgumentException 当 clazz 为 null 时抛出
     * @throws RuntimeException 当对象创建或属性拷贝失败时抛出
     */
    public static <E> E create(Class<E> clazz, Map<String, ?> map) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }

        try {
            E entity = clazz.getDeclaredConstructor().newInstance();

            if (map != null && !map.isEmpty()) {
                PropertyUtils.copyProperties(entity, map);
            }

            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Create entity from map failed, class: " + clazz.getName(), e);
        }
    }

    /**
     * 将当前实体对象转换为 Map。
     * <p>
     * 默认会过滤掉以下属性：
     * <ul>
     *     <li>{@code class} 属性</li>
     *     <li>{@code null} 值属性</li>
     *     <li>空字符串属性</li>
     * </ul>
     * 如果传入 keys，则只保留 keys 中指定的属性；如果未传入 keys，则保留所有非空属性。
     * </p>

     * @param keys 需要保留的属性名列表，不传则保留所有非空属性
     * @return 当前对象对应的属性 Map
     * @throws RuntimeException 当实体转换为 Map 失败时抛出
     */
    public Map<String, Object> toMap(String... keys) {
        try {
            Map<String, Object> map = PropertyUtils.describe(this);
            map.remove("class");

            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();

                if (isEmptyValue(entry.getValue())) {
                    iterator.remove();
                    continue;
                }

                if (!containsKey(entry.getKey(), keys)) {
                    iterator.remove();
                }
            }

            return map;
        } catch (Exception e) {
            throw new RuntimeException("Convert entity to map failed, class: " + this.getClass().getName(), e);
        }
    }

    /**
     * 判断属性名是否包含在指定的 keys 数组中。
     * <p>
     * 判定规则：
     * <ul>
     *     <li>key 为空时，默认返回 true</li>
     *     <li>keys 为空时，表示不限制字段，默认返回 true</li>
     *     <li>keys 不为空时，只有 key 存在于 keys 中才返回 true</li>
     * </ul>
     * </p>
     *
     * @param key  当前属性名
     * @param keys 允许保留的属性名数组
     * @return 若允许保留当前属性则返回 {@code true}，否则返回 {@code false}
     */
    private boolean containsKey(Object key, String[] keys) {
        String keyStr = Objects.toString(key, "");

        if (StringUtil.isEmpty(keyStr)) {
            return true;
        }

        if (keys == null || keys.length == 0) {
            return true;
        }

        return Arrays.stream(keys)
                .filter(Objects::nonNull)
                .anyMatch(keyStr::equals);
    }

    /**
     * 判断属性值是否为空。
     * <p>
     * 判定规则：
     * <ul>
     *     <li>{@code null} 视为空</li>
     *     <li>空字符串/空白字符序列视为空</li>
     * </ul>
     * </p>
     *
     * @param value 属性值
     * @return 若属性值为空则返回 {@code true}，否则返回 {@code false}
     */
    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof CharSequence charSequence) {
            return StringUtil.isEmpty(charSequence.toString());
        }

        return false;
    }

    /**
     * 从 Map 中拷贝属性到当前对象。
     * <p>
     * Map 中的 key 需要与当前对象的属性名保持一致。
     * 该方法会直接修改当前对象，并返回当前对象本身（链式调用）。
     * </p>
     *
     * @param source 属性 Map，若为空则直接返回当前对象
     * @param <E>    当前实体类型
     * @return 当前对象本身
     * @throws RuntimeException 当属性拷贝失败时抛出
     */
    @SuppressWarnings("unchecked")
    public <E extends PeachDO> E copyFromMap(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return (E) this;
        }

        try {
            PropertyUtils.copyProperties(this, source);
            return (E) this;
        } catch (Exception e) {
            throw new RuntimeException("Copy properties from map failed, class: " + this.getClass().getName(), e);
        }
    }

    /**
     * 克隆当前对象。
     * <p>
     * 该方法基于 Apache Commons BeanUtils 的 {@code cloneBean} 实现。
     * 注意：该方法主要用于 JavaBean 的浅层/单层属性克隆，对于包含复杂嵌套集合的对象，非严格意义上的深拷贝。
     * </p>
     *
     * @param <E> 当前实体类型
     * @return 当前对象的克隆副本
     * @throws RuntimeException 当克隆失败时抛出
     */
    @SuppressWarnings("unchecked")
    public <E extends PeachDO> E deepClone() {
        try {
            return (E) BeanUtils.cloneBean(this);
        } catch (Exception e) {
            throw new RuntimeException("Clone entity failed, class: " + this.getClass().getName(), e);
        }
    }

    /**
     * 显式填充创建审计字段（创建时间和创建人 ID）。
     *
     * @param creatorId 创建人 ID
     */
    public void fillCreateTime(String creatorId) {
        this.createdTime = getCurrentTime();
        this.creatorId = creatorId;
    }

    /**
     * 从当前安全上下文自动填充创建审计字段、租户及组织信息。
     * <p>
     * 说明：框架内部通过反射调用安全上下文以避免直接包依赖。
     * 若实体定义了 {@code tenantId} 或 {@code orgId} 可写属性，会自动进行填充和非空校验。
     * </p>
     *
     * @throws IllegalStateException 当租户或组织信息在当前上下文中缺失时抛出
     */
    public void fillCreateTime() {
        fillCreateTime(currentContextValue(CURRENT_USER_ID_FIELD));
        fillCurrentTenantOrg();
        requireTenantOrgIfPresent();
    }

    /**
     * 从当前安全上下文自动填充创建审计字段，并显式指定租户及组织 ID。
     *
     * @param tenantId 租户 ID
     * @param orgId    组织 ID
     * @throws IllegalStateException 当租户或组织信息缺失时抛出
     */
    public void fillCreateTime(String tenantId, String orgId) {
        fillCreateTime(currentContextValue(CURRENT_USER_ID_FIELD));
        fillTenantOrg(tenantId, orgId);
    }

    /**
     * 显式填充修改审计字段（修改时间和修改人 ID）。
     *
     * @param modifierId 修改人 ID
     */
    public void fillModifyTime(String modifierId) {
        this.modifyTime = getCurrentTime();
        this.modifierId = modifierId;
    }

    /**
     * 从当前安全上下文自动填充修改审计字段（修改时间和修改人 ID）。
     */
    public void fillModifyTime() {
        fillModifyTime(currentContextValue(CURRENT_USER_ID_FIELD));
    }

    /**
     * 从当前安全上下文自动填充租户 ID 和组织 ID（若实体存在对应可写属性）。
     */
    public void fillCurrentTenantOrg() {
        setPropertyIfWritable(TENANT_ID_FIELD, currentContextValue("currentTenantId"));
        setPropertyIfWritable(ORG_ID_FIELD, currentContextValue("currentOrgId"));
    }

    /**
     * 显式填充租户 ID 和组织 ID（若实体存在对应可写属性），并触发非空校验。
     *
     * @param tenantId 租户 ID
     * @param orgId    组织 ID
     * @throws IllegalStateException 当传入的租户或组织 ID 为空时抛出
     */
    public void fillTenantOrg(String tenantId, String orgId) {
        setPropertyIfWritable(TENANT_ID_FIELD, tenantId);
        setPropertyIfWritable(ORG_ID_FIELD, orgId);
        requireTenantOrgIfPresent();
    }

    /**
     * 校验实体中的租户 ID 和组织 ID 属性（如果实体包含这些可写属性，则值不能为空）。
     *
     * @throws IllegalStateException 当对应的租户或组织 ID 属性值为 null 或空字符串时抛出
     */
    public void requireTenantOrgIfPresent() {
        requirePropertyIfWritable(TENANT_ID_FIELD, "Current tenant context is missing");
        requirePropertyIfWritable(ORG_ID_FIELD, "Current organization context is missing");
    }

    /**
     * 获取当前系统时间字符串。
     *
     * @return 当前格式化后的时间字符串
     */
    public static String getCurrentTime() {
        return DateUtil.nowTime();
    }

    /**
     * 校验日期字符串格式是否合法。
     *
     * @param dateStr 待校验的日期字符串
     * @return 若合法则返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isValidDate(String dateStr) {
        return DateUtil.isValidDate(dateStr);
    }

    /**
     * 获取当前对象指定属性的值。
     *
     * @param field 属性名，不能为空
     * @return 属性值
     * @throws IllegalArgumentException 当 field 为空时抛出
     * @throws RuntimeException 当读取属性值失败时抛出
     */
    public Object getField(String field) {
        checkFieldName(field);

        try {
            return PropertyUtils.getProperty(this, field);
        } catch (Exception e) {
            throw new RuntimeException("Get field value failed, field: " + field, e);
        }
    }

    /**
     * 获取当前对象指定属性的值，并转换为指定的类型。
     *
     * @param field 属性名，不能为空
     * @param type  目标类型的 Class，不能为空
     * @param <E>   返回值类型泛型
     * @return 转换为指定类型后的属性值；若原属性值为 null 则返回 null
     * @throws IllegalArgumentException 当 field 或 type 为空时抛出
     * @throws ClassCastException 当类型转换失败时抛出
     * @throws RuntimeException 当读取属性值失败时抛出
     */
    public <E> E getValue(String field, Class<E> type) {
        checkFieldName(field);

        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        try {
            Object value = PropertyUtils.getProperty(this, field);

            if (value == null) {
                return null;
            }

            return type.cast(value);
        } catch (Exception e) {
            throw new RuntimeException("Get field value failed, field: " + field, e);
        }
    }

    /**
     * 设置当前对象指定属性的值。
     *
     * @param field 属性名，不能为空
     * @param value 要设置的属性值
     * @throws IllegalArgumentException 当 field 为空时抛出
     * @throws RuntimeException 当设置属性值失败时抛出
     */
    public void setValue(String field, Object value) {
        checkFieldName(field);

        try {
            PropertyUtils.setProperty(this, field, value);
        } catch (Exception e) {
            throw new RuntimeException("Set field value failed, field: " + field, e);
        }
    }

    /**
     * 通过反射从安全上下文（{@code SecurityContextHolder}）中获取静态无参方法的返回值。
     *
     * @param methodName 无参静态方法名
     * @return 方法调用的字符串结果；若类/方法不存在或返回值为 null/空串，则返回 {@code null}
     * @throws RuntimeException 当反射调用过程中发生非反射找不到的异常时抛出
     */
    private static String currentContextValue(String methodName) {
        try {
            Class<?> holderClass = Class.forName(SECURITY_CONTEXT_HOLDER_CLASS);
            Object value = holderClass.getMethod(methodName).invoke(null);
            if (value == null || StringUtil.isEmpty(value.toString())) {
                return null;
            }
            return value.toString();
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Get current security context failed, method: " + methodName, e);
        }
    }

    /**
     * 若当前对象存在指定名称的可写属性，则为其设置属性值。
     *
     * @param field 属性名
     * @param value 属性值
     * @throws RuntimeException 当设置属性失败时抛出
     */
    private void setPropertyIfWritable(String field, String value) {
        if (!PropertyUtils.isWriteable(this, field)) {
            return;
        }
        try {
            PropertyUtils.setProperty(this, field, value);
        } catch (Exception e) {
            throw new RuntimeException("Set context field failed, field: " + field, e);
        }
    }

    /**
     * 若当前对象存在指定名称的可写属性，则校验其属性值不能为空。
     *
     * @param field   属性名
     * @param message 校验失败时的异常信息
     * @throws IllegalStateException 当该属性可写且属性值为空（null 或空字符串）时抛出
     */
    private void requirePropertyIfWritable(String field, String message) {
        if (!PropertyUtils.isWriteable(this, field)) {
            return;
        }
        Object value = getField(field);
        if (value == null || StringUtil.isEmpty(value.toString())) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 校验属性名是否为空。
     *
     * @param field 属性名
     * @throws IllegalArgumentException 当属性名为 null 或空字符串时抛出
     */
    private void checkFieldName(String field) {
        if (ObjectUtils.isEmpty(field)) {
            throw new IllegalArgumentException("field must not be empty");
        }
    }

    /**
     * 从当前类及其父类中递归查找带有 {@link Id} 注解的主键字段。
     *
     * @param clazz 待查找的实体 Class
     * @return 标注了 {@link Id} 的 Field 对象
     * @throws IllegalArgumentException 当传入 Class 为 null、Object.class、PeachDO.class 或未找到 @Id 字段时抛出
     */
    private static Field getIdField(Class<?> clazz) {
        if (clazz == null || clazz == Object.class || clazz == PeachDO.class) {
            throw new IllegalArgumentException("No field annotated with @Id found");
        }

        Optional<Field> idField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(Id.class) != null)
                .findFirst();

        return idField.orElseGet(() -> getIdField(clazz.getSuperclass()));
    }
}
