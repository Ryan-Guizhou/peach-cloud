package com.peach.scheduler.service.impl;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.service.ISchedulerJobService;
import com.peach.scheduler.service.SchedulerJobLifecycleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.scheduled.common.JobEvent;
import com.peach.scheduled.common.JobState;
import com.peach.scheduled.common.SyncStatus;
import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerOperationLogDao;
import com.peach.scheduler.dao.SchedulerJobVersionDao;
import com.peach.scheduled.dto.SchedulerJobSaveDTO;
import com.peach.scheduled.entity.SchedulerHandlerDO;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduler.model.ScheduleType;
import com.peach.scheduled.qo.SchedulerJobQO;
import com.peach.scheduled.vo.SchedulerJobVO;
import org.springframework.beans.BeanUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.quartz.CronExpression;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度任务管理服务实现。
 *
 * <p>负责任务定义校验、Handler 在线校验、参数安全校验、版本快照、
 * 生命周期调用以及 DO 到 VO 的边界转换。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerJobServiceImpl implements ISchedulerJobService {
    private static final int MAX_PARAMETER_LENGTH = 16384;
    private static final Set<String> DENIED_PARAMETER_NAMES = new HashSet<String>(Arrays.asList(
            "password", "passwd", "token", "secret", "accesskey", "access_key",
            "secretkey", "secret_key", "privatekey", "private_key", "credential", "credentials"));
    private final SchedulerJobDao jobDao;
    private final SchedulerHandlerDao handlerDao;
    private final SchedulerJobVersionDao jobVersionDao;
    private final SchedulerJobLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;
    private final SchedulerOperationLogDao operationLogDao;

    /**
     * 创建相关对象。
     * @param jobDao 任务定义数据访问对象
     * @param handlerDao Handler 注册数据访问对象
     * @param jobVersionDao 任务定义版本快照数据访问对象
     * @param lifecycleService 生命周期服务
     * @param objectMapper JSON 序列化器
     * @param operationLogDao 操作审计日志数据访问对象
     */
    public SchedulerJobServiceImpl(SchedulerJobDao jobDao,
                               SchedulerHandlerDao handlerDao,
                               SchedulerJobVersionDao jobVersionDao,
                               SchedulerJobLifecycleService lifecycleService,
                               ObjectMapper objectMapper,
                               SchedulerOperationLogDao operationLogDao) {
        this.jobDao = jobDao;
        this.handlerDao = handlerDao;
        this.jobVersionDao = jobVersionDao;
        this.lifecycleService = lifecycleService;
        this.objectMapper = objectMapper;
        this.operationLogDao = operationLogDao;
    }

    /**
     * 创建相关对象。
     * @param data 任务定义请求
     * @param operatorId 操作人 ID
     * @return 创建后的任务定义
     */
    @Transactional
    @Override
    public SchedulerJobVO create(SchedulerJobSaveDTO data, String operatorId) {
        validate(data);
        if (jobDao.selectByCode(data.getJobCode()) != null) {
            throw new IllegalArgumentException("Job code already exists");
        }
        SchedulerJobDO job = map(data, new SchedulerJobDO());
        job.setState(JobState.DRAFT);
        job.setScheduleVersion(1L);
        job.setSyncStatus(SyncStatus.PENDING);
        job.setVersion(0L);
        job.setCreatorId(operatorId);
        job.setModifierId(operatorId);
        jobDao.insert(job);
        jobVersionDao.insertSnapshot(job);
        operationLogDao.insertSuccess("CREATE", "JOB", String.valueOf(job.getId()), operatorId, null);
        return toVO(job);
    }

    /**
     * 更新相关数据。
     * @param jobId 任务 ID
     * @param data 任务定义请求
     * @param operatorId 操作人 ID
     * @return 更新后的任务定义
     */
    @Transactional
    @Override
    public SchedulerJobVO update(Long jobId, SchedulerJobSaveDTO data, String operatorId) {
        validate(data);
        SchedulerJobDO job = required(jobId);
        if (!job.getJobCode().equals(data.getJobCode())) {
            throw new IllegalArgumentException("Job code cannot be changed after creation");
        }
        map(data, job);
        job.setModifierId(operatorId);
        if (jobDao.updateDefinition(job) != 1) {
            throw new IllegalStateException("Concurrent scheduler job update detected");
        }
        SchedulerJobDO refreshed = required(jobId);
        jobVersionDao.insertSnapshot(refreshed);
        operationLogDao.insertSuccess("UPDATE", "JOB", String.valueOf(jobId), operatorId, null);
        return toVO(refreshed);
    }

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     * @param query 查询条件
     */
    @Override
    public List<SchedulerJobVO> list(SchedulerJobQO query) {
        return toVOList(jobDao.selectPage(query));
    }

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     * @param jobId 任务 ID
     */
    @Override
    public SchedulerJobVO get(Long jobId) {
        return toVO(required(jobId));
    }

    /**
     * 执行相关状态处理。
     *
     * @return 返回结果
     * @param jobId 任务 ID
     * @param event 状态机事件
     * @param operatorId 操作人 ID
     */
    @Transactional
    @Override
    public SchedulerJobVO transition(Long jobId, JobEvent event, String operatorId) {
        SchedulerJobDO job = lifecycleService.transition(jobId, event, operatorId);
        operationLogDao.insertSuccess(event.name(), "JOB", String.valueOf(jobId), operatorId, null);
        return toVO(job);
    }

    private SchedulerJobVO toVO(SchedulerJobDO job) {
        SchedulerJobVO vo = new SchedulerJobVO();
        BeanUtils.copyProperties(job, vo);
        return vo;
    }

    private List<SchedulerJobVO> toVOList(List<SchedulerJobDO> jobs) {
        java.util.ArrayList<SchedulerJobVO> result = new java.util.ArrayList<SchedulerJobVO>(jobs.size());
        for (SchedulerJobDO job : jobs) {
            result.add(toVO(job));
        }
        return result;
    }

    private void validateScheduleType(SchedulerJobSaveDTO data, ScheduleType type) {
        if (type == ScheduleType.CRON && !CronExpression.isValidExpression(data.getCronExpression())) {
            throw new IllegalArgumentException("Invalid Quartz cron expression");
        }
        if (type == ScheduleType.FIXED_INTERVAL && (data.getIntervalSeconds() == null || data.getIntervalSeconds() <= 0)) {
            throw new IllegalArgumentException("intervalSeconds must be positive");
        }
        if (type == ScheduleType.ONE_TIME && (data.getStartAt() == null || data.getStartAt().isBlank())) {
            throw new IllegalArgumentException("startAt is required for ONE_TIME schedule");
        }
    }

    private void validateExecutionPolicy(SchedulerJobSaveDTO data) {
        if (data.getTimeoutMs() == null || data.getTimeoutMs() < 1000L || data.getTimeoutMs() > 86400000L) {
            throw new IllegalArgumentException("timeoutMs must be between 1000 and 86400000");
        }
        if (data.getMaxAttempts() == null || data.getMaxAttempts() < 1 || data.getMaxAttempts() > 20) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and 20");
        }
        if (data.getRetryIntervalSeconds() == null
                || data.getRetryIntervalSeconds() < 1
                || data.getRetryIntervalSeconds() > 86400) {
            throw new IllegalArgumentException("retryIntervalSeconds must be between 1 and 86400");
        }
    }

    private void validate(SchedulerJobSaveDTO data) {
        SchedulerHandlerDO handler = handlerDao.selectOne(data.getApplicationName(), data.getHandlerName());
        if (handler == null || !"ONLINE".equals(handler.getStatus())) {
            throw new IllegalArgumentException("Handler is not registered and online for target application");
        }
        ScheduleType type;
        try {
            type = ScheduleType.valueOf(data.getScheduleType());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unsupported schedule type", ex);
        }
        validateScheduleType(data, type);
        try {
            ZoneId.of(data.getTimeZone());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid IANA time zone", ex);
        }
        validateExecutionPolicy(data);
        validateParameters(data.getParametersJson());
    }

    private void validateParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) return;
        if (parametersJson.length() > MAX_PARAMETER_LENGTH) {
            throw new IllegalArgumentException("parametersJson exceeds 16 KiB limit");
        }
        try {
            JsonNode root = objectMapper.readTree(parametersJson);
            rejectSensitiveKeys(root);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("parametersJson must be valid JSON");
        }
    }

    private void rejectSensitiveKeys(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (DENIED_PARAMETER_NAMES.contains(field.getKey().toLowerCase())) {
                    throw new IllegalArgumentException("Sensitive credential fields are not allowed in scheduler parameters");
                }
                rejectSensitiveKeys(field.getValue());
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) rejectSensitiveKeys(child);
        }
    }

    private SchedulerJobDO map(SchedulerJobSaveDTO data, SchedulerJobDO job) {
        job.setJobCode(data.getJobCode());
        job.setJobName(data.getJobName());
        job.setApplicationName(data.getApplicationName());
        job.setHandlerName(data.getHandlerName());
        job.setDescription(data.getDescription());
        job.setScheduleType(data.getScheduleType());
        job.setCronExpression(data.getCronExpression());
        job.setIntervalSeconds(data.getIntervalSeconds());
        job.setStartAt(data.getStartAt() == null || data.getStartAt().isBlank()
                ? null : LocalDateTime.parse(data.getStartAt()));
        job.setTimeZone(data.getTimeZone());
        job.setMisfirePolicy(data.getMisfirePolicy());
        job.setConcurrencyPolicy(data.getConcurrencyPolicy());
        job.setTimeoutMs(data.getTimeoutMs());
        job.setMaxAttempts(data.getMaxAttempts());
        job.setRetryIntervalSeconds(data.getRetryIntervalSeconds());
        job.setParametersJson(data.getParametersJson());
        return job;
    }

    private SchedulerJobDO required(Long jobId) {
        SchedulerJobDO job = jobDao.selectById(jobId);
        if (job == null) throw new IllegalArgumentException("Scheduler job not found: " + jobId);
        return job;
    }
}
