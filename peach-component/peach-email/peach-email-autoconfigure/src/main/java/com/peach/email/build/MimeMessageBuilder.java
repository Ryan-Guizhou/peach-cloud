package com.peach.email.build;

import com.peach.email.core.Attachment;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.InlineResource;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:05
 * @Description
 * 将领域模型 EmailMessage 构建为可发送的 MimeMessage。
 * 结构采用 multipart/mixed + multipart/alternative (+ multipart/related) 组合
 * 以兼容多客户端并支持附件与内嵌资源。
 */
public class MimeMessageBuilder {

    /** 构建 MimeMessage */
    public static MimeMessage build(Session session, EmailMessage message) throws Exception {
        MimeMessage mime = new MimeMessage(session);
        mime.setFrom(new InternetAddress(message.getFrom()));

        for (String s : message.getTo()) {
            mime.addRecipients(Message.RecipientType.TO, InternetAddress.parse(s));
        }
        for (String t : message.getCc()) {
            mime.addRecipients(Message.RecipientType.CC, InternetAddress.parse(t));
        }
        for (String t : message.getBcc()) {
            mime.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(t));
        }
        if (message.getReplyTo() != null) {
            mime.setReplyTo(new InternetAddress[]{new InternetAddress(message.getReplyTo())});
        }
        mime.setSubject(message.getSubject(), "UTF-8");
        if (message.getHeaders() != null) {
            for (Map.Entry<String,String> e : message.getHeaders().entrySet()) {
                mime.addHeader(e.getKey(), e.getValue());
            }
        }

        boolean hasAttachments = message.getAttachments() != null && !message.getAttachments().isEmpty();
        boolean hasInline = message.getInlineResources() != null && !message.getInlineResources().isEmpty();
        boolean hasHtml = message.getHtml() != null;
        boolean hasText = message.getText() != null;

        if (hasAttachments || hasInline) {
            MimeMultipart mixed = new MimeMultipart("mixed");
            MimeBodyPart alternativePartHolder = new MimeBodyPart();
            MimeMultipart alternative = new MimeMultipart("alternative");
            if (hasText) {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(message.getText(), "UTF-8");
                alternative.addBodyPart(textPart);
            }
            if (hasHtml) {
                MimeBodyPart htmlPartBody = new MimeBodyPart();
                htmlPartBody.setContent(message.getHtml(), "text/html; charset=UTF-8");
                if (hasInline) {
                    MimeMultipart related = new MimeMultipart("related");
                    MimeBodyPart htmlPart = new MimeBodyPart();
                    htmlPart.setContent(message.getHtml(), "text/html; charset=UTF-8");
                    related.addBodyPart(htmlPart);
                    addInlineResources(related, message.getInlineResources());
                    MimeBodyPart relatedHolder = new MimeBodyPart();
                    relatedHolder.setContent(related);
                    alternative.addBodyPart(relatedHolder);
                } else {
                    alternative.addBodyPart(htmlPartBody);
                }
            }
            alternativePartHolder.setContent(alternative);
            mixed.addBodyPart(alternativePartHolder);
            addAttachments(mixed, message.getAttachments());
            mime.setContent(mixed);
        } else {
            if (hasHtml && hasText) {
                MimeMultipart alternative = new MimeMultipart("alternative");
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(message.getText(), "UTF-8");
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(message.getHtml(), "text/html; charset=UTF-8");
                alternative.addBodyPart(textPart);
                alternative.addBodyPart(htmlPart);
                mime.setContent(alternative);
            } else if (hasHtml) {
                mime.setContent(message.getHtml(), "text/html; charset=UTF-8");
            } else if (hasText) {
                mime.setText(message.getText(), "UTF-8");
            }
        }
        mime.saveChanges();
        return mime;
    }

    /** 添加附件到 mixed 部分 */
    private static void addAttachments(MimeMultipart mixed, List<Attachment> attachments) throws Exception {
        if (attachments == null) {
            return;
        }
        for (Attachment a : attachments) {
            MimeBodyPart part = new MimeBodyPart();
            DataSource ds;
            if (a.getContent() != null) {
                ds = new ByteArrayDataSource(a.getContent(), a.getContentType() != null ? a.getContentType() : "application/octet-stream");
            } else if (a.getPath() != null) {
                ds = new FileDataSource(new File(a.getPath()));
            } else {
                continue;
            }
            part.setDataHandler(new DataHandler(ds));
            if (a.getFilename() != null) {
                part.setFileName(a.getFilename());
            }
            if (a.getDisposition() != null) {
                part.setDisposition(a.getDisposition());
            }
            mixed.addBodyPart(part);
        }
    }

    /** 添加内嵌资源到 related 部分（通过 CID 引用） */
    private static void addInlineResources(MimeMultipart related, List<InlineResource> inlineResources) throws Exception {
        if (inlineResources == null) return;
        for (InlineResource r : inlineResources) {
            MimeBodyPart part = new MimeBodyPart();
            DataSource ds;
            if (r.getContent() != null) {
                ds = new ByteArrayDataSource(r.getContent(), r.getContentType() != null ? r.getContentType() : "application/octet-stream");
            } else if (r.getPath() != null) {
                ds = new FileDataSource(new File(r.getPath()));
            } else {
                continue;
            }
            part.setDataHandler(new DataHandler(ds));
            if (r.getContentId() != null) {
                part.setHeader("Content-ID", "<" + r.getContentId() + ">");
            }
            related.addBodyPart(part);
        }
    }

}
