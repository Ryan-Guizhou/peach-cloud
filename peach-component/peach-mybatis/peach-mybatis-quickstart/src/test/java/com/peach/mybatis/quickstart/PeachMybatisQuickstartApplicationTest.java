package com.peach.mybatis.quickstart;

import com.peach.mybatis.interceptor.SqlCostInterceptor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 starter 能启动并注册多库识别与 SQL 日志。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@SpringBootTest
class PeachMybatisQuickstartApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldRegisterDatabaseDetectionAndSqlLogging() {
        assertThat(context.getBean(DatabaseIdProvider.class)).isNotNull();
        assertThat(context.getBean(SqlCostInterceptor.class)).isNotNull();
    }
}
