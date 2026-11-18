package com.peach.satoken.filter;

import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.security.SatokenEndpointRule;
import com.peach.satoken.support.UserContextSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 服务专用调用不得落入普通用户资料缓存，且无 Same-Token 必须直接拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:15
 */
class UserContextServiceTokenOnlyTest {

    @Test
    void shouldKeepInternalOnlyConfigurationEmptyByDefault() {
        UserContextProperties properties = new UserContextProperties();
        assertThat(properties.getServiceTokenOnlyEndpoints()).isEmpty();
    }

    @Test
    void shouldRejectUntrustedServiceWithoutLoadingLegacyUserProfile() throws Exception {
        UserContextProperties properties = new UserContextProperties();
        properties.setServiceTokenOnlyEndpoints(List.of(
                SatokenEndpointRule.of("POST", "/auth/external/iam/check-api")));
        UserContextSupport support = mock(UserContextSupport.class);
        UserContextFilter filter = new UserContextFilter(properties, support);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/auth/external/iam/check-api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(support);
    }
}
