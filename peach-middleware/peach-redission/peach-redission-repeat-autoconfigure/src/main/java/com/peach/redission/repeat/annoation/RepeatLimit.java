package com.peach.redission.repeat.annoation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:11
 */
@Documented
@Target(value= {ElementType.TYPE, ElementType.METHOD})
@Retention(value= RetentionPolicy.RUNTIME)
public @interface RepeatLimit {

    /**
     * 业务名称
     * @return name
     */
    String name() default "";
    /**
     * key设置,支持spl表达式
     * @return key
     */
    String [] keys();

    /**
     * 在多长时间内一直保持幂等，如果不配置则以执行方法为准，时间单位默认是(秒/s)
     * */
    long durationTime() default 0L;

    /**
     * 当消息执行已经出发防重复执行的限制时，提示信息
     * */
    String message() default "Submission is too frequent. Please try again later";
}
