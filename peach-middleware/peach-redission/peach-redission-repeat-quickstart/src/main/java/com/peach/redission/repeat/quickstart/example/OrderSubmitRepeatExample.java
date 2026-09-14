package com.peach.redission.repeat.quickstart.example;

import com.peach.redission.repeat.annoation.RepeatLimit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 注解方式：{@code @RepeatLimit} 按下单 requestId 防重复提交。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Service
public class OrderSubmitRepeatExample {

    /**
     * 提交订单；相同 requestId 在窗口期内仅允许成功一次。
     *
     * @param requestId 请求幂等键
     * @return 提交结果
     */
    @RepeatLimit(name = "order-submit", keys = {"#p0"}, durationTime = 30L,
            message = "Duplicate order submit is not allowed")
    public String submitOrder(String requestId) {
        log.info("order submit accepted, requestId={}", requestId);
        return "accepted:" + requestId;
    }
}
