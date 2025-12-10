package com.peach.sample.email;

import com.peach.email.autoconfigure.EmailProperties;
import com.peach.email.core.Attachment;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.InlineResource;
import com.peach.email.core.SendResult;
import com.peach.email.service.EmailSendService;
import com.peach.email.template.TemplateManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 10:59
 */
@Component
public class EmailSendUtil {

    @Autowired
    private EmailSendService EmailSendService;

    @Autowired
    private TemplateManager templateManager;

    public SendResult sendSimple() {
        EmailMessage message = EmailMessage.builder().from("445623047@qq.com")
                .to(Arrays.asList("517651412@qq.com"))
                .subject("测试")
                .text("测试")
                .cc(Arrays.asList("445623047@qq.com"))
                .html("<h1>测试</h1>")
                .build();
        SendResult sendResult = EmailSendService.sendAuto(message);
        return sendResult;
    }

    public SendResult sendComplex() throws IOException {
        Map<String,Object> data = new HashMap<String,Object>();
        data.put("name", "示例用户");
        data.put("logoCid", "logoCid");
        String html = templateManager.renderById("qq_complex", data);

        List<InlineResource> inlines = new ArrayList<InlineResource>();
        if (Files.exists(Paths.get("C:\\Users\\pc\\Downloads\\logo.jpg"))) {
            inlines.add(new InlineResource("logoCid", "image/png", Files.readAllBytes(Paths.get("logo.png")), null));
        }

        List<Attachment> atts = new ArrayList<Attachment>();
        if (Files.exists(Paths.get("C:\\Users\\pc\\Downloads\\说明.docx"))) {
            atts.add(new Attachment("说明.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Files.readAllBytes(Paths.get("C:\\Users\\pc\\Downloads\\说明.docx")), null, "attachment"));
        }

        EmailMessage.Builder b = EmailMessage.builder()
                .from("445623047@qq.com")
                .to(Arrays.asList("huanhuanshu48@gmail.com"))
                .subject("QQ复杂邮件示例")
                .text("文本降级内容")
                .html(html)
                .inlineResources(inlines)
                .attachments(atts)
                .idempotencyKey("demo-" + "445623047@qq.com");


        EmailMessage message = b.build();
        SendResult r = EmailSendService.sendAuto(message);
        return r;
    }
}
