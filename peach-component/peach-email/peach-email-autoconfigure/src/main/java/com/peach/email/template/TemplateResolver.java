package com.peach.email.template;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description  模板解析接口：将模板ID解析为classpath路径
 */
public interface TemplateResolver {

    /**
     * 根据模板ID返回classpath路径，无法解析时返回null
     */
    String resolve(String templateId);
}
