package com.peach.threadpool.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWrapperTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPropagateAndRestoreMdc() {
        TaskWrapper wrapper = new TaskWrapper(true, false);
        MDC.put("requestId", "request-123");
        Runnable wrapped = wrapper.wrap(() -> {
            assertThat(MDC.get("requestId")).isEqualTo("request-123");
            MDC.put("workerOnly", "value");
        });

        MDC.clear();
        MDC.put("worker", "existing");
        wrapped.run();

        assertThat(MDC.get("worker")).isEqualTo("existing");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("workerOnly")).isNull();
    }

    @Test
    void shouldPropagateMdcForCallable() throws Exception {
        TaskWrapper wrapper = new TaskWrapper(true, false);
        MDC.put("traceId", "trace-123");
        AtomicReference<String> observed = new AtomicReference<>();

        String result = wrapper.wrap(() -> {
            observed.set(MDC.get("traceId"));
            return "done";
        }).call();

        assertThat(result).isEqualTo("done");
        assertThat(observed).hasValue("trace-123");
    }
}
