package com.peach.sample.email.resolver;

import com.peach.email.template.TemplateResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义模板解析器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 11:17
 * @Description 自定义模板解析器
 */
public class CustomTemplateResolver implements TemplateResolver {

    protected static final Map<String,String> map = new HashMap<>();

    public CustomTemplateResolver() {
        map.put("qq_complex", "qq_complex.ftl");
    }

    @Override
    public String resolve(String templateId) {
        return map.get(templateId);
    }
}
