package com.peach.gateway.core.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关不得将来自其他租户、应用、年度或旧版本的远程许可误当成当前许可。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:55
 */
class GatewayIamHttpDecisionVerifierTest {

    private final GatewayTokenContext snapshot =
            new GatewayTokenContext("user", "tenant", "member", "app", 2026, 2, 7, null);

    @Test
    void shouldAllowOnlyExactVerifiedPrincipalAndVersions() {
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "app", 2026, 2, 7, true))).isTrue();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("different-tenant", "app", 2026, 2, 7, true))).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "other-app", 2026, 2, 7, true))).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "app", 2027, 2, 7, true))).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "app", 2026, 2, 8, true))).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "app", 2026, 3, 7, true))).isFalse();
    }

    @Test
    void shouldRejectNoEvidenceOrDeniedAction() {
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot, null)).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                new GatewayIamHttpDecisionVerifier.IamApiDecision(true, null))).isFalse();
        assertThat(GatewayIamHttpDecisionVerifier.isMatchingDecision(snapshot,
                decision("tenant", "app", 2026, 2, 7, false))).isFalse();
    }

    private static GatewayIamHttpDecisionVerifier.IamApiDecision decision(
            String tenant, String app, int year, long memberVersion, long authVersion,
            boolean allowed) {
        GatewayIamHttpDecisionVerifier.IamVerifiedContext context =
                new GatewayIamHttpDecisionVerifier.IamVerifiedContext(
                        "user", tenant, "member", app, year, memberVersion, authVersion);
        return new GatewayIamHttpDecisionVerifier.IamApiDecision(allowed, context);
    }
}
