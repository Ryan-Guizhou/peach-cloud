package com.peach.common;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:42
 */
@Data
public class PeachEntity implements Serializable {

    @Schema(description = "分页页码")
    private Integer pageNum = 1;

    @Schema(description = "分页大小")
    private Integer pageSize = 20;

    @SuppressWarnings("unchecked")
    public <T extends PeachEntity> T clone(Map<?, ?> source) {
        try {
            BeanUtils.copyProperties(this, source);
            return (T) this;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Failed to clone entity properties", ex);
        }
    }

    public static <T extends PeachEntity> T create(Class<T> c) {
        try {
            return c.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to instantiate entity", ex);
        }
    }
}
