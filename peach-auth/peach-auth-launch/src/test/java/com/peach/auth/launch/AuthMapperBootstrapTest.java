package com.peach.auth.launch;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证实际打包的全部认证 Mapper 均能解析，防止失效类型引用阻断登录启动。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
class AuthMapperBootstrapTest {
    @Test
    void shouldParseEveryAuthMapperIncludingTenant() throws Exception {
        var configuration = new Configuration();
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:com/peach/auth/dao/*.xml");
        assertThat(resources).isNotEmpty();
        for (var resource : resources) {
            try (var stream = resource.getInputStream()) {
                new XMLMapperBuilder(stream, configuration, resource.toString(), configuration.getSqlFragments()).parse();
            }
        }
        assertThat(configuration.getMappedStatementNames()).contains("com.peach.auth.dao.TenantDao.selectByQO");
        var loginSql = configuration.getMappedStatement("com.peach.auth.dao.UserOrgDao.selectByUserId")
                .getBoundSql(java.util.Map.of("userId", "test-user")).getSql();
        assertThat(loginSql).contains("t.DELETED", "t.STATUS", "'ACTIVE'").doesNotContain("t.IS_DELETE", "t.SORT_NUM");
    }
}
