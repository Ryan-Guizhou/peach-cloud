package com.peach.common;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
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

    public <T extends PeachEntity> T clone(Map source) {
        try {
            BeanUtils.copyProperties(this, source);
            return (T) this;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends PeachEntity> T create(Class<T> c) throws RuntimeException {
        try {
            T t = c.newInstance();
            return t;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
