package com.peach.observability.quickstart.scenario;

import com.peach.observability.core.ObservabilityConstants;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可观测性演示场景：读取当前 RequestId 并组装响应。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class ObservabilityScenarioService {

    /**
     * 组装 ping 响应，包含当前 MDC 中的 requestId。
     *
     * @return 响应体
     */
    public Map<String, String> ping() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        String requestId = MDC.get(ObservabilityConstants.REQUEST_ID_MDC_KEY);
        body.put("requestId", requestId == null ? "" : requestId);
        return body;
    }
}
