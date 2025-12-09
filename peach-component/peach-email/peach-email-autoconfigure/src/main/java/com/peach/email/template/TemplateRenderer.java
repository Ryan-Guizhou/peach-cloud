package com.peach.email.template;

import java.util.Map;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 模板渲染接口
 */
public interface TemplateRenderer {

    /**
     * 按路径渲染模板并返回字符串
     */
    String render(String templatePath, Map<String,Object> data);
}
