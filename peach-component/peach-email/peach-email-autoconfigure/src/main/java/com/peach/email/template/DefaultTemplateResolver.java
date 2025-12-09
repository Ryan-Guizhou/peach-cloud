package com.peach.email.template;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 预置模板解析器：提供内置模板ID到路径的映射
 */
public class DefaultTemplateResolver implements TemplateResolver {

    private final Map<String,String> map = new HashMap<String,String>();

    public DefaultTemplateResolver() {
        map.put("birthday", "template-birthday.ftl");
    }

    public String resolve(String templateId) {
        return map.get(templateId);
    }
}
