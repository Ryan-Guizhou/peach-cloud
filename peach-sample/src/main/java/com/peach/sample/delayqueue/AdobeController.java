package com.peach.sample.delayqueue;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.redission.delayqueue.context.DelayQueueContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Adobe控制器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:55
 */
@Indexed
@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class AdobeController {

        private final DelayQueueContext context;


    @GetMapping("/send")
    public String send(){
        for (int i = 100000; i > 0; i--) {
            context.sendMessage("delay-demo-queue","发送信息"+i,10, TimeUnit.MILLISECONDS);
        }
        return "success";
    }
}
