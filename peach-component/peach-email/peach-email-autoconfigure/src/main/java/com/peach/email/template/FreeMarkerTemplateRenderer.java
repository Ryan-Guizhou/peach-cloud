package com.peach.email.template;

import com.peach.email.constant.EmailConstant;
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
        this.cfg = new Configuration(Configuration.VERSION_2_3_31);
        this.cfg.setDefaultEncoding(EmailConstant.TEMPLATES_ENCODING);
        this.cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), EmailConstant.TEMPLATES_PATH);
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
