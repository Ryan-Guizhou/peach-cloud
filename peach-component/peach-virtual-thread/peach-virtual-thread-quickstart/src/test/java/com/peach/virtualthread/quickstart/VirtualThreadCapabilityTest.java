package com.peach.virtualthread.quickstart;

import com.peach.virtualthread.exception.VirtualTaskRejectReason;
import com.peach.virtualthread.quickstart.example.VirtualCancelExample;
import com.peach.virtualthread.quickstart.example.VirtualGroupSubmitExample;
import com.peach.virtualthread.quickstart.example.VirtualRejectExample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@code VirtualExecutorService} 的分组提交、受管取消与 REJECT 背压。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@SpringBootTest(properties = "quickstart.virtual-thread.demo.enabled=false")
class VirtualThreadCapabilityTest {

    private static final String PAYLOAD = "it-virtual-thread";

    @Autowired
    private VirtualGroupSubmitExample submitExample;

    @Autowired
    private VirtualCancelExample cancelExample;

    @Autowired
    private VirtualRejectExample rejectExample;

    @Test
    void shouldSubmitCallableOnDatabaseGroup() {
        VirtualGroupSubmitExample.SubmitObservation observed = submitExample.submitCallable(PAYLOAD);
        assertThat(observed.getPayload()).isEqualTo(PAYLOAD);
        assertThat(observed.getGroupName()).isEqualTo(VirtualGroupSubmitExample.GROUP);
        assertThat(observed.getThreadName()).startsWith("peach-vt-database-");
    }

    @Test
    void shouldCancelManagedSupplyAsync() {
        VirtualCancelExample.CancelObservation observed = cancelExample.cancelManagedSupply();
        assertThat(observed.isCancelRequested()).isTrue();
        assertThat(observed.isFutureCancelled()).isTrue();
    }

    @Test
    void shouldRejectWhenBurstCapacityFull() {
        VirtualRejectExample.RejectObservation observed = rejectExample.rejectWhenCapacityFull();
        assertThat(observed.getGroup()).isEqualTo(VirtualRejectExample.GROUP);
        assertThat(observed.getReason()).isEqualTo(VirtualTaskRejectReason.CAPACITY_FULL);
        assertThat(observed.getRejectedDelta()).isGreaterThanOrEqualTo(1L);
    }
}
