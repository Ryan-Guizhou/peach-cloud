package com.peach.email.template;

import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 模板管理器：聚合SPI与Spring注入的解析器并委托渲染器输出
 */
@Slf4j
public class TemplateManager {

    private final TemplateRenderer renderer;

    private final List<TemplateResolver> resolvers = new ArrayList<TemplateResolver>();

    public TemplateManager(TemplateRenderer renderer, List<TemplateResolver> extraResolvers) {
        this.renderer = renderer;
        if (extraResolvers != null && extraResolvers.size() > 0) {
            resolvers.addAll(extraResolvers);
        }
        ServiceLoader<TemplateResolver> loader = ServiceLoader.load(TemplateResolver.class);
        Iterator<TemplateResolver> it = loader.iterator();
        while (it.hasNext()) {
            resolvers.add(it.next());
        }
        resolvers.add(new DefaultTemplateResolver());
    }

    /** 通过模板ID渲染 */
    public String renderById(String templateId, Map<String,Object> data) {
        String path = resolve(templateId);
        if (path == null) {
            log.info("template id not found: " + templateId);
            throw new IllegalArgumentException("template id not found: " + templateId);
        }
        return renderer.render(path, data);
    }

    /** 解析模板ID到路径 */
    private String resolve(String templateId) {
        for (TemplateResolver r : resolvers) {
            String p = r.resolve(templateId);
            if (StringUtil.isNotBlank(p)) {
                return p;
            }
        }
        return null;
    }
}
