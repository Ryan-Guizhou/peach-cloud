package com.peach.scheduler.service.impl;

import com.peach.scheduled.qo.SchedulerExecutionQO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证筛选 SQL 的 MyBatis 绑定边界。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 21:50
 */
class SchedulerGovernanceMapperTest {
    @Test
    void handlerFiltersShouldBeOptionalAndBound() throws Exception {
        var configuration = load("SchedulerHandlerDao");
        var statement = configuration.getMappedStatement("com.peach.scheduler.dao.SchedulerHandlerDao.selectFiltered");
        var query = new com.peach.scheduled.qo.SchedulerHandlerQO();
        assertThat(statement.getBoundSql(query).getSql()).doesNotContain("STATUS =");
        query.setApplicationName("peach-auth");
        query.setStatus("OFFLINE");
        query.setKeyword("' OR 1=1 --");
        var sql = statement.getBoundSql(query);
        assertThat(sql.getSql()).doesNotContain(query.getKeyword()).doesNotContain("ENABLED");
        assertThat(sql.getParameterMappings()).extracting(mapping -> mapping.getProperty())
                .contains("applicationName", "status", "keyword");
    }

    private Configuration load(String name) throws Exception {
        Configuration configuration = new Configuration();
        String resource = "com/peach/scheduler/dao/" + name + ".xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    @Test
    void executionFiltersShouldBindValuesIncludingParsedDates() throws Exception {
        var configuration = load("SchedulerExecutionDao");
        SchedulerExecutionQO query = new SchedulerExecutionQO();
        query.setExecutionId("' OR 1=1 --");
        query.setTriggerType("MANUAL");
        query.setExecutorInstance("worker");
        query.setScheduledFrom("2026-11-21T00:00:00");
        query.setScheduledTo("2026-11-22T00:00:00");
        var sql = configuration.getMappedStatement("com.peach.scheduler.dao.SchedulerExecutionDao.selectPage")
                .getBoundSql(query);
        assertThat(sql.getSql()).doesNotContain(query.getExecutionId());
        assertThat(sql.getParameterMappings()).extracting(mapping -> mapping.getProperty())
                .contains("executionId", "triggerType", "executorInstance", "scheduledFromValue", "scheduledToValue");
    }
}
