package com.peach.email.core;

import java.util.Optional;
import java.util.Properties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description
 * 传输上下文：承载 SMTP 连接所需的主机、端口、SSL 与认证信息，
 * 可通过 extra 注入额外 JavaMail 属性（如超时等）。
 */
public class EmailContext {

    /**
     * SMTP 主机名（必填或使用默认）
     */
    private final String host;

    /**
     * SMTP 端口（必填或使用默认）
     */
    private final int port;

    /**
     * 是否启用 SSL（true）或 STARTTLS（false）
     */
    private final boolean ssl;

    /**
     * 认证用户名（邮箱地址）
     */
    private final String username;

    /**
     * 授权码或密码
     */
    private final String password;

    /**
     * 额外 JavaMail 属性（建议只读使用）；内部保存为防御性拷贝
     */
    private final Properties extra;

    public EmailContext(String host, int port, boolean ssl, String username, String password, Properties extra) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.username = username;
        this.password = password;
        Properties copy = new Properties();
        Optional.ofNullable(extra).ifPresent(copy::putAll);
        this.extra = copy;
    }

    public String getHost() { return host; }

    public int getPort() { return port; }

    public boolean isSsl() { return ssl; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public Properties getExtra() {
        Properties p = new Properties();
        Optional.ofNullable(extra).ifPresent(p::putAll);
        return p;
    }
}
