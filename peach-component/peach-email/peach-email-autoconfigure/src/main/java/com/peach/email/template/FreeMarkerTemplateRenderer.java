package com.peach.email.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.Version;

import java.io.StringWriter;
import java.util.Map;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 基于 FreeMarker 的模板渲染实现
 */
public class FreeMarkerTemplateRenderer implements TemplateRenderer {
    private final Configuration cfg;

    public FreeMarkerTemplateRenderer() {
        this.cfg = new Configuration(new Version(2,3,31));
        this.cfg.setDefaultEncoding("UTF-8");
        this.cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/");
    }

    public String render(String templatePath, Map<String, Object> data) {
        try {
            Template t = cfg.getTemplate(templatePath);
            StringWriter sw = new StringWriter();
            t.process(data, sw);
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
