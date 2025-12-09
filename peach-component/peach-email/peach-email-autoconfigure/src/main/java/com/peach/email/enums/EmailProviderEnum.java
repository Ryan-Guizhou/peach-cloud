package com.peach.email.enums;

public enum EmailProviderEnum {
    ALI("ali", "smtpdm.aliyun.com","465"),
    NETEASE_163("163", "smtp.163.com","465"),
    QQ("qq", "smtp.qq.com","465"),
    GMAIL("gmail", "smtp.gmail.com","465");

    private final String name;

    private final String smtpServer;

    private final String port;

    EmailProviderEnum(String name, String smtpServer,String port) {
        this.name = name;
        this.smtpServer = smtpServer;
        this.port = port;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public String getPort() {
        return port;
    }

    public String getName() {
        return name;
    }


}