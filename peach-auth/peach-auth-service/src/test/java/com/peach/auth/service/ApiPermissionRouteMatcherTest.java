package com.peach.auth.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 目录匹配先确定唯一最具体路由，再判断角色证据，任何歧义拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 13:35
 */
class ApiPermissionRouteMatcherTest {

    @Test
    void shouldNormalizeConcreteApiMethodAndDenyEncodedOrWildcardPaths() {
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("get:/api/users/123"))
                .isEqualTo("GET:/api/users/123");
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("GET:/api/users/{id}")).isNull();
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("GET:/api/users/%2e%2e")).isNull();
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("GET:/api/users//1")).isNull();
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("GET:/api/../users")).isNull();
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("OPTIONS:/api/users/1")).isNull();
        assertThat(ApiPermissionRouteMatcher.normalizeRequestCode("GET:/api/users/1?a=1")).isNull();
    }

    @Test
    void shouldResolveSegmentTemplateWithoutCrossingPathBoundaries() {
        List<String> catalog = List.of("GET:/api/users/{userId}", "GET:/api/users/secret");
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/users/123", catalog))
                .isEqualTo("GET:/api/users/{userId}");
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/users/secret", catalog))
                .isEqualTo("GET:/api/users/secret");
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/users/123/edit", catalog)).isNull();
        assertThat(ApiPermissionRouteMatcher.resolve("POST:/api/users/123", catalog)).isNull();
    }

    @Test
    void shouldResolveNestedRoleAndPermissionPhysicalIdRoutes() {
        String actual = "PUT:/api/auth/saas/roles/"
                + "1234567890abcdef1234567890abcdef/permissions/"
                + "abcdef1234567890abcdef1234567890/data-policies";
        String template =
                "PUT:/api/auth/saas/roles/{roleRefId}/permissions/{permissionRefId}/data-policies";
        assertThat(ApiPermissionRouteMatcher.resolve(actual, List.of(template)))
                .isEqualTo(template);
        assertThat(ApiPermissionRouteMatcher.resolve(
                actual.replace("data-policies", "field-policies"), List.of(template))).isNull();
    }

    @Test
    void shouldDenyAmbiguousOrUnsafeCatalogPatterns() {
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/files/preview", List.of(
                "GET:/api/files/{id}", "GET:/api/{type}/preview"))).isNull();
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/secret", List.of(
                "GET:/api/{id:[a-z]+}", "GET:/api/**"))).isNull();
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/files/1", List.of(
                "GET:/api/files/{id}", "GET:/api/files/{name}"))).isNull();
    }

    @Test
    void shouldDenyTruncatedCatalogEvenIfFirstRecordWouldGrant() {
        List<String> codes = new ArrayList<>();
        codes.add("GET:/api/files/{id}");
        for (int i = 0; i < 512; i++) {
            codes.add("GET:/other/" + i);
        }
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/files/123", codes)).isNull();
    }

    @Test
    void shouldDeduplicateIdenticalCatalogEntries() {
        assertThat(ApiPermissionRouteMatcher.resolve("GET:/api/files/123", List.of(
                "GET:/api/files/{id}", "GET:/api/files/{id}")))
                .isEqualTo("GET:/api/files/{id}");
    }
}
