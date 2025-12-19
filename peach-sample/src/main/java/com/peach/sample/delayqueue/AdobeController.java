package com.peach.sample.delayqueue;

import com.peach.redission.delayqueue.context.DelayQueueContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:55
 */
@RestController
@RequestMapping("/queue")
public class AdobeController {

    @Autowired
    private DelayQueueContext context;


    @RequestMapping("/send")
    public String send(){
        for (int i = 100000; i > 0; i--) {
            context.sendMessage("delay-demo-queue","发送信息"+i,10, TimeUnit.MILLISECONDS);
        }
        return "success";
    }
}
