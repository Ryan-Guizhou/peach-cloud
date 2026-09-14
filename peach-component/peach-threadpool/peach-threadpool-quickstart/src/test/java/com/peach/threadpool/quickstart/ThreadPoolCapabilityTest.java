package com.peach.threadpool.quickstart;

import com.peach.threadpool.quickstart.example.AsyncExecutedExample;
import com.peach.threadpool.quickstart.example.ThreadPoolExecuteExample;
import com.peach.threadpool.quickstart.example.ThreadPoolSubmitExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ThreadPoolManager 提交/执行以及 {@code @AsyncExecuted} 走受管 IO 池。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:57
 */
@SpringBootTest(properties = "quickstart.threadpool.demo.enabled=false")
class ThreadPoolCapabilityTest {

    private static final String PAYLOAD = "it-threadpool";

    @Autowired
    private ThreadPoolSubmitExample submitExample;

    @Autowired
    private ThreadPoolExecuteExample executeExample;

    @Autowired
    private AsyncExecutedExample asyncExecutedExample;

    @Test
    void shouldSubmitCallableOnManagedPool() {
        ThreadPoolSubmitExample.SubmitObservation observed = submitExample.submitCallable(PAYLOAD);
        assertThat(observed.getPayload()).isEqualTo(PAYLOAD);
        assertThat(observed.getThreadName()).startsWith("peach-pool-io-");
    }

    @Test
    void shouldExecuteAndSubmitRunnableOnManagedPool() {
        String worker = executeExample.executeRunnable();
        assertThat(worker).startsWith("peach-pool-io-");
        executeExample.submitRunnable();
    }

    @Test
    void shouldRunAsyncExecutedOnManagedPool() {
        String worker = asyncExecutedExample.runOnIoPool();
        assertThat(worker).startsWith("peach-pool-io-");
    }
}
