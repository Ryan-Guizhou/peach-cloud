package com.peach.email.router;

import com.peach.common.loader.CustomServiceLoader;
import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:43
 * @Description
 * 提供商路由器：维护可用的传输实现、上下文与优先级，
 * 提供按优先级排序的候选集合以支持故障转移策略。
 */
public class ProviderRouter {

    /**
     * 提供商名称到传输实现的映射
     */
    private final Map<String, EmailTransport> transports = new HashMap<String, EmailTransport>();

    /**
     * 提供商名称到上下文（主机、端口、账号）的映射
     */
    private final Map<String, EmailContext> contexts = new HashMap<String, EmailContext>();

    /**
     * 提供商优先级，值越小优先级越高
     */
    private final Map<String, Integer> priorities = new HashMap<String, Integer>();

    public ProviderRouter() {
        ServiceLoader<EmailTransport> loader = ServiceLoader.load(EmailTransport.class);
        Iterator<EmailTransport> it = loader.iterator();
        while (it.hasNext()) {
            EmailTransport t = it.next();
            transports.put(t.getName(), t);
            if (!priorities.containsKey(t.getName())) priorities.put(t.getName(), 100);
        }
    }

    public void register(String name, EmailTransport transport, EmailContext context) {
        transports.put(name, transport);
        contexts.put(name, context);
    }

    /** 设置提供商上下文（主机、端口、账号） */
    public void setContext(String name, EmailContext context) {
        contexts.put(name, context);
    }

    /** 设置提供商优先级（越小越优先） */
    public void setPriority(String name, int priority) {
        priorities.put(name, priority);
    }

    public Integer getPriority(String name) { return priorities.get(name); }

    public Set<String> getProviderNames() { return transports.keySet(); }

    public EmailTransport getTransport(String name) { return transports.get(name); }

    public EmailContext getContext(String name) { return contexts.get(name); }

    /** 按名称发送 */
    public SendResult send(String name, EmailMessage message) {
        EmailTransport t = transports.get(name);
        EmailContext c = contexts.get(name);
        if (t == null || c == null) throw new IllegalStateException("provider not configured: " + name);
        return t.send(message, c);
    }

    /**
     * 根据优先级返回提供商列表，优先级值越小越靠前。
     * 如果 candidates 为空，则返回全部已注册提供商。
     */
    public List<String> orderByPriority(List<String> candidates) {
        List<String> names = new ArrayList<String>();
        if (candidates == null || candidates.isEmpty()) names.addAll(transports.keySet());
        else names.addAll(candidates);
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) {
                int pa = priorities.containsKey(a) ? priorities.get(a) : 100;
                int pb = priorities.containsKey(b) ? priorities.get(b) : 100;
                return Integer.compare(pa, pb);
            }
        });
        return names;
    }
}
