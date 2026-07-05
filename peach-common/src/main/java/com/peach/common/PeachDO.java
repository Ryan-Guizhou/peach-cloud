package com.peach.common;

import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基础数据库实体对象。
 *
 * <p>
 * 该类主要用于承载所有数据库实体通用的审计字段和基础工具方法。
 * 所有 DO / Entity 实体可以继承该类，统一获得创建人、创建时间、更新人、更新时间等基础字段。
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 *     <li>该类不建议承担 DTO、VO 转换职责，DTO/DO/VO 转换建议交给 Converter / Assembler。</li>
 *     <li>该类不建议包含具体业务默认值逻辑，业务默认值应由子类或业务层处理。</li>
 *     <li>该类提供的反射方法主要用于通用框架能力，业务代码中应谨慎使用。</li>
 * </ul>
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/6 20:30
 */
@Data
public class PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 缓存每个实体类上的主键字段，避免重复反射扫描。
     */
    private static final Map<Class<?>, Field> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Created time.
     */
    @Column(name = "CREATED_TIME")
    @Schema(description = "创建时间")
    private String createdTime;

    /**
     * Creator id.
     */
    @Column(name = "CREATOR_ID")
    @Schema(description = "创建人ID")
    private String creatorId;

    /**
     * Modify time.
     */
    @Column(name = "MODIFY_TIME")
    @Schema(description = "修改时间")
    private String modifyTime;

    /**
     * Modifier id.
     */
    @Column(name = "MODIFIER_ID")
    @Schema(description = "修改人ID")
    private String modifierId;


    /**
     * 根据 Map 创建指定类型的对象。
     *
     * <p>
     * Map 中的 key 需要与目标对象的属性名保持一致。
     * 该方法适用于简单对象属性拷贝，不建议用于复杂嵌套对象转换。
     * </p>
     *
     * @param clazz 目标对象类型
     * @param map   属性 Map
     * @param <E>   目标对象泛型
     * @return 创建并填充属性后的对象
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
     * 将当前对象转换为 Map。
     *
     * <p>
     * 默认会过滤掉以下属性：
     * <ul>
     *     <li>class 属性</li>
     *     <li>null 值属性</li>
     *     <li>空字符串属性</li>
     * </ul>
     * </p>
     *
     * <p>
     * 如果传入 keys，则只保留 keys 中指定的属性。
     * 如果未传入 keys，则保留所有非空属性。
     * </p>
     *
     * @param keys 需要保留的属性名，可为空
     * @return 当前对象对应的属性 Map
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
     * 判断属性名是否包含在指定 keys 中。
     *
     * <p>
     * 规则：
     * <ul>
     *     <li>key 为空时，默认返回 true</li>
     *     <li>keys 为空时，表示不限制字段，默认返回 true</li>
     *     <li>keys 不为空时，只有 key 存在于 keys 中才返回 true</li>
     * </ul>
     * </p>
     *
     * @param key  当前属性名
     * @param keys 允许保留的属性名数组
     * @return 是否允许保留当前属性
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
     *
     * <p>
     * 当前规则：
     * <ul>
     *     <li>null 视为空</li>
     *     <li>空字符串视为空</li>
     * </ul>
     * </p>
     *
     * @param value 属性值
     * @return 是否为空
     */
    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof CharSequence) {
            return StringUtil.isEmpty(value.toString());
        }

        return false;
    }

    /**
     * 从 Map 中拷贝属性到当前对象。
     *
     * <p>
     * Map 中的 key 需要与当前对象的属性名保持一致。
     * 该方法会修改当前对象，并返回当前对象本身。
     * </p>
     *
     * @param source 属性 Map
     * @param <E>    当前实体类型
     * @return 当前对象
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
     * 深拷贝当前对象。
     *
     * <p>
     * 该方法基于 Apache Commons BeanUtils 的 cloneBean 实现。
     * 注意：该方法更适合 JavaBean 的浅层属性克隆，对于复杂嵌套对象集合，不一定是真正意义上的深拷贝。
     * </p>
     *
     * @param <E> 当前实体类型
     * @return 当前对象的克隆对象
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
     * 为当前实体设置主键 ID。
     *
     * <p>
     * 该方法会查找当前类或父类中带有 {@link Id} 注解的字段，并为其赋值 UUID。
     * </p>
     *
     * <p>
     * 注意：
     * <ul>
     *     <li>如果当前实体已经有 ID，则不会覆盖。</li>
     *     <li>如果实体类及其父类都没有 {@link Id} 注解字段，则会抛出异常。</li>
     * </ul>
     * </p>
     *
     * @param <E> 当前实体类型
     * @return 当前对象
     */
    @SuppressWarnings("unchecked")
//    public <E extends PeachDO> E setEntityId() {
//        try {
//            Field idField = initIdField(this.getClass());
//            Object currentValue = idField.get(this);
//
//            if (currentValue == null || StringUtil.isEmpty(currentValue.toString())) {
//                idField.set(this, IDGeneratorUtil.UUID());
//            }
//
//            return (E) this;
//        } catch (Exception e) {
//            throw new RuntimeException("Set entity id failed, class: " + this.getClass().getName(), e);
//        }
//    }

    /**
     * Fill create audit fields.
     *
     * @param creatorId creator id
     */
    public void fillCreateTime(String creatorId) {
        this.createdTime = getCurrentTime();
        this.creatorId = creatorId;
    }

    /**
     * Fill modify audit fields.
     *
     * @param modifierId modifier id
     */
    public void fillModifyTime(String modifierId) {
        this.modifyTime = getCurrentTime();
        this.modifierId = modifierId;
    }



    /**
     * 获取当前系统时间字符串。
     *
     * @return 当前时间字符串
     */
    public static String getCurrentTime() {
        return DateUtil.nowTime();
    }

    /**
     * 校验日期字符串是否合法。
     *
     * @param dateStr 日期字符串
     * @return 是否合法
     */
    public static boolean isValidDate(String dateStr) {
        return DateUtil.isValidDate(dateStr);
    }

    /**
     * 获取当前对象指定字段的值。
     *
     * @param field 字段名
     * @return 字段值
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
     * 获取当前对象指定字段的值，并转换为指定类型。
     *
     * @param field 字段名
     * @param type  目标类型
     * @param <E>   返回值类型
     * @return 字段值
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
     * 设置当前对象指定字段的值。
     *
     * @param field 字段名
     * @param value 字段值
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
     * 校验字段名是否为空。
     *
     * @param field 字段名
     */
    private void checkFieldName(String field) {
        if (ObjectUtils.isEmpty(field)) {
            throw new IllegalArgumentException("field must not be empty");
        }
    }

    /**
     * 初始化并缓存实体主键字段。
     *
     * @param clazz 实体 Class
     * @return 主键字段
     */
//    private static Field initIdField(Class<?> clazz) {
//        return ID_FIELD_CACHE.computeIfAbsent(clazz, key -> {
//            Field idField = getIdField(key);
//            idField.setAccessible(true);
//            return idField;
//        });
//    }

    /**
     * 从当前类及其父类中查找带有 {@link Id} 注解的字段。
     *
     * @param clazz 实体 Class
     * @return 主键字段
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
