package com.peach.redission.repeat.quickstart.scenario;

import com.peach.redission.repeat.annoation.RepeatLimit;
import org.springframework.stereotype.Service;

/**
 * 下单防重复场景：演示 {@code @RepeatLimit}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class OrderSubmitRepeatScenarioService {

    /**
     * 提交订单；相同 requestId 在窗口期内仅允许成功一次。
     *
     * @param requestId 请求幂等键
     * @return 提交结果
     */
    @RepeatLimit(name = "order-submit", keys = {"#requestId"}, durationTime = 30L,
            message = "Duplicate order submit is not allowed")
    public String submitOrder(String requestId) {
        return "accepted:" + requestId;
    }
}
