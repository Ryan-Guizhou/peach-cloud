package com.peach.email.build;

import com.peach.email.core.Attachment;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.InlineResource;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.List;
import java.util.Map;

/**
 * 将领域模型 EmailMessage 构建为可发送的 MimeMessage。
 * 结构采用 multipart/mixed + multipart/alternative (+ multipart/related) 组合
 * 以兼容多客户端并支持附件与内嵌资源。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:05
 */
public class MimeMessageBuilder {

    private MimeMessageBuilder() {
        throw new IllegalStateException("Utility class");
    }

    private static final String HTML_UTF8_CONTENT_TYPE = "text/html; charset=UTF-8";

    private static final String UTF8_CHARSET = "UTF-8";


    /** 构建 MimeMessage */
    public static MimeMessage build(Session session, EmailMessage message) throws MessagingException {
        MimeMessage mime = new MimeMessage(session);
        applyHeaders(mime, message);
        applyBody(mime, message);
        mime.saveChanges();
        return mime;
    }

    private static void applyHeaders(MimeMessage mime, EmailMessage message) throws MessagingException {
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
        mime.setSubject(message.getSubject(), UTF8_CHARSET);
        if (message.getHeaders() != null) {
            for (Map.Entry<String,String> e : message.getHeaders().entrySet()) {
                mime.addHeader(e.getKey(), e.getValue());
            }
        }
    }

    private static void applyBody(MimeMessage mime, EmailMessage message) throws MessagingException {
        boolean hasAttachments = message.getAttachments() != null && !message.getAttachments().isEmpty();
        boolean hasInline = message.getInlineResources() != null && !message.getInlineResources().isEmpty();
        boolean hasHtml = message.getHtml() != null;
        boolean hasText = message.getText() != null;

        if (hasAttachments || hasInline) {
            mime.setContent(buildMixedContent(message, hasAttachments, hasInline, hasHtml, hasText));
            return;
        }
        applySimpleBody(mime, message, hasHtml, hasText);
    }

    private static MimeMultipart buildMixedContent(EmailMessage message, boolean hasAttachments,
                                                   boolean hasInline, boolean hasHtml, boolean hasText)
            throws MessagingException {
        MimeMultipart mixed = new MimeMultipart("mixed");
        MimeBodyPart alternativePartHolder = new MimeBodyPart();
        alternativePartHolder.setContent(buildAlternativeContent(message, hasInline, hasHtml, hasText));
        mixed.addBodyPart(alternativePartHolder);
        if (hasAttachments) {
            addAttachments(mixed, message.getAttachments());
        }
        return mixed;
    }

    private static MimeMultipart buildAlternativeContent(EmailMessage message, boolean hasInline,
                                                         boolean hasHtml, boolean hasText) throws MessagingException {
        MimeMultipart alternative = new MimeMultipart("alternative");
        if (hasText) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(message.getText(), UTF8_CHARSET);
            alternative.addBodyPart(textPart);
        }
        if (hasHtml) {
            if (hasInline) {
                MimeMultipart related = new MimeMultipart("related");
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(message.getHtml(), HTML_UTF8_CONTENT_TYPE);
                related.addBodyPart(htmlPart);
                addInlineResources(related, message.getInlineResources());
                MimeBodyPart relatedHolder = new MimeBodyPart();
                relatedHolder.setContent(related);
                alternative.addBodyPart(relatedHolder);
            } else {
                MimeBodyPart htmlPartBody = new MimeBodyPart();
                htmlPartBody.setContent(message.getHtml(), HTML_UTF8_CONTENT_TYPE);
                alternative.addBodyPart(htmlPartBody);
            }
        }
        return alternative;
    }

    private static void applySimpleBody(MimeMessage mime, EmailMessage message, boolean hasHtml, boolean hasText)
            throws MessagingException {
        if (hasHtml && hasText) {
            MimeMultipart alternative = new MimeMultipart("alternative");
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(message.getText(), UTF8_CHARSET);
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(message.getHtml(), HTML_UTF8_CONTENT_TYPE);
            alternative.addBodyPart(textPart);
            alternative.addBodyPart(htmlPart);
            mime.setContent(alternative);
        } else if (hasHtml) {
            mime.setContent(message.getHtml(), HTML_UTF8_CONTENT_TYPE);
        } else if (hasText) {
            mime.setText(message.getText(), UTF8_CHARSET);
        }
    }

    /** 添加附件到 mixed 部分 */
    private static void addAttachments(MimeMultipart mixed, List<Attachment> attachments) throws MessagingException {
        if (attachments == null) {
            return;
        }
        for (Attachment a : attachments) {
            MimeBodyPart part = new MimeBodyPart();
            DataSource ds;
            if (a.content() != null) {
                ds = new ByteArrayDataSource(a.content(), a.contentType() != null ? a.contentType() : "application/octet-stream");
            } else if (a.path() != null) {
                ds = new FileDataSource(a.path());
            } else {
                continue;
            }
            part.setDataHandler(new DataHandler(ds));
            if (a.filename() != null) {
                part.setFileName(a.filename());
            }
            if (a.disposition() != null) {
                part.setDisposition(a.disposition());
            }
            mixed.addBodyPart(part);
        }
    }

    /** 添加内嵌资源到 related 部分（通过 CID 引用） */
    private static void addInlineResources(MimeMultipart related, List<InlineResource> inlineResources) throws MessagingException {
        if (inlineResources == null) return;
        for (InlineResource r : inlineResources) {
            MimeBodyPart part = new MimeBodyPart();
            DataSource ds;
            if (r.content() != null) {
                ds = new ByteArrayDataSource(r.content(), r.contentType() != null ? r.contentType() : "application/octet-stream");
            } else if (r.path() != null) {
                ds = new FileDataSource(r.path());
            } else {
                continue;
            }
            part.setDataHandler(new DataHandler(ds));
            if (r.contentId() != null) {
                part.setHeader("Content-ID", "<" + r.contentId() + ">");
            }
            related.addBodyPart(part);
        }
    }

}
