package com.peach.sample.email;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.email.core.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 10:57
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailSendController {

        private final EmailSendUtil emailSendUtil;

    /**
     * 发送简单邮件
     * @return
     */
    @GetMapping("/sendSimple")
    public SendResult sendSimple() {
        SendResult sendResult = emailSendUtil.sendSimple();
        log.info("sendResult = {}", sendResult);
        return sendResult;
    }

    /**
     * 发送复杂邮件
     * @return
     */
    @GetMapping("/sendComplex")
    public SendResult sendComplex() throws IOException {
        SendResult sendResult = emailSendUtil.sendComplex();
        log.info("sendResult = {}", sendResult);
        return sendResult;
    }
}
