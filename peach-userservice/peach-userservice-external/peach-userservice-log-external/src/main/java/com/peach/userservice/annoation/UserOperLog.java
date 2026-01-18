package com.peach.userservice.annoation;

import com.peach.common.util.StringUtil;
import com.peach.userservice.enums.UserLogEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 16:39
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface UserOperLog {

    /**
     * 模块编码
     * @return
     */
    UserLogEnum.Module moduleCode() default UserLogEnum.Module.DEFAULT;

    /**
     * 操作类型
     * @return
     */
    UserLogEnum.OptType optType() default UserLogEnum.OptType.DEFAULT;

    /**
     * 用户操作了什么内容
     * @return
     */
    String optContent() default StringUtil.EMPTY;

    /**
     * 操作级别 INSERT:INFO UPDATE:DEBUG DELETE:WARN
     * @return
     */
    UserLogEnum.LogLevel optLevel() default UserLogEnum.LogLevel.DEFAULT;

}
