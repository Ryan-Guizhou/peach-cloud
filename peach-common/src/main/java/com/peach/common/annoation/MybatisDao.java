package com.peach.common.annoation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/13 11:52
 * @Description 标记Mapper层 用于扫描
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MybatisDao {

}
