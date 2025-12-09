package com.peach.email.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description
 * 业务邮件模型：统一描述发件人、收件人、主题与内容，以及附件、内嵌资源与自
 * 支持提供幂等键以避免重复外发；使用构建器创建不可变对象。
 */
public class EmailMessage {
    /**
     * 发件人邮箱地址
     */
    private final String from;

    /**
     * 收件人列表
     */
    private final List<String> to;

    /**
     * 抄送人列表
     */
    private final List<String> cc;

    /**
     * 密送人列表
     */
    private final List<String> bcc;

    /**
     * 邮件主题
     */
    private final String subject;

    /**
     * 纯文本正文（作为 HTML 的降级）
     */
    private final String text;

    /**
     * HTML 正文
     */
    private final String html;

    /**
     * 附件集合
     */
    private final List<Attachment> attachments;

    /**
     * 内嵌资源集合（通过 CID 引用）
     */
    private final List<InlineResource> inlineResources;

    /**
     * 自定义头部
     */
    private final Map<String, String> headers;

    /**
     * 回复地址
     */
    private final String replyTo;

    /**
     * 幂等键（相同业务不可重复外发）
     */
    private final String idempotencyKey;

    private EmailMessage(Builder b) {
        this.from = b.from;
        this.to = Collections.unmodifiableList(new ArrayList<String>(b.to));
        this.cc = Collections.unmodifiableList(new ArrayList<String>(b.cc));
        this.bcc = Collections.unmodifiableList(new ArrayList<String>(b.bcc));
        this.subject = b.subject;
        this.text = b.text;
        this.html = b.html;
        this.attachments = Collections.unmodifiableList(new ArrayList<Attachment>(b.attachments));
        this.inlineResources = Collections.unmodifiableList(new ArrayList<InlineResource>(b.inlineResources));
        this.headers = b.headers;
        this.replyTo = b.replyTo;
        this.idempotencyKey = b.idempotencyKey;
    }

    public String getFrom() {
        return from;
    }

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public String getHtml() {
        return html;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public List<InlineResource> getInlineResources() {
        return inlineResources;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器：用于组装邮件对象
     */
    public static class Builder {

        /**
         * 发件人邮箱地址（必填）
         */
        private String from;

        /**
         * 收件人列表（至少一个）
         */
        private List<String> to = new ArrayList<String>();

        /**
         * 抄送人列表（选填）
         */
        private List<String> cc = new ArrayList<String>();

        /**
         * 密送人列表（选填）
         */
        private List<String> bcc = new ArrayList<String>();

        /**
         * 邮件主题（推荐）
         */
        private String subject;

        /**
         * 纯文本正文（推荐，作为降级内容）
         */
        private String text;

        /**
         * HTML 正文（选填）
         */
        private String html;

        /**
         * 附件集合（选填）
         */
        private List<Attachment> attachments = new ArrayList<Attachment>();

        /**
         * 内嵌资源集合（选填）
         */
        private List<InlineResource> inlineResources = new ArrayList<InlineResource>();

        /**
         * 自定义头部（选填）
         */
        private Map<String, String> headers;

        /**
         * 回复地址（选填）
         */
        private String replyTo;

        /**
         * 幂等键（选填，推荐）
         */
        private String idempotencyKey;

        public Builder from(String v) {
            this.from = v;
            return this;
        }

        public Builder to(List<String> v) {
            this.to = v;
            return this;
        }

        public Builder cc(List<String> v) {
            this.cc = v;
            return this;
        }

        public Builder bcc(List<String> v) {
            this.bcc = v;
            return this;
        }

        public Builder subject(String v) {
            this.subject = v;
            return this;
        }

        public Builder text(String v) {
            this.text = v;
            return this;
        }

        public Builder html(String v) {
            this.html = v;
            return this;
        }

        public Builder attachments(List<Attachment> v) {
            this.attachments = v;
            return this;
        }
        public Builder addAttachment(Attachment v) {
            this.attachments.add(v);
            return this;
        }
        public Builder addAttachment(String filename, String contentType,  String disposition) {
            this.attachments.add(new Attachment(filename, contentType, disposition));
            return this;
        }
        public Builder inlineResources(List<InlineResource> v) {
            this.inlineResources = v;
            return this;
        }
        public Builder addInlineResource(InlineResource v) {
            this.inlineResources.add(v);
            return this;
        }

        public Builder headers(Map<String,String> v) {
            this.headers = v;
            return this;
        }

        public Builder replyTo(String v) {
            this.replyTo = v;
            return this;
        }

        public Builder idempotencyKey(String v) {
            this.idempotencyKey = v;
            return this;
        }
        /** 生成不可变的邮件对象 */
        public EmailMessage build() {
            return new EmailMessage(this);
        }
    }
}
