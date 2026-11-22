package com.peach.auth.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.peach.auth.dao.LoginLogDao;
import com.peach.auth.entity.LoginLogDO;
import com.peach.auth.qo.LoginLogQO;
import com.peach.auth.service.support.LoginAuditContext;
import com.peach.auth.service.support.LoginAuditProperties;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * 登录日志只读查询与保留期清理；客户端不能指定其他租户或绕过平台权限。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Service
@RequiredArgsConstructor
public class LoginLogService {
    private final LoginLogDao dao;
    private final LoginAuditProperties properties;

    /** 当前租户查询，或经显式平台授权查询未归属记录；单次时间跨度不得超过90天。 */
    public PageInfo<LoginLogDO> pageList(LoginLogQO query) {
        UserContext actor = authorized();
        if ("UNASSIGNED".equals(query.getScope()) && dao.hasPermission(actor, "loginLog:platform") == 0) {
            throw new BusinessException("无未归属登录事件的平台审计权限");
        }
        if (!"TENANT".equals(query.getScope()) && !"UNASSIGNED".equals(query.getScope())) throw new BusinessException("无效的日志查询范围");
        query.setTenantId(actor.getTenantId());
        query.setAccountHash(query.getAccount() == null || query.getAccount().isBlank() ? null : LoginAuditContext.hashAccount(query.getAccount()));
        LocalDateTime now = LocalDateTime.now();
        if (query.getStartTime() == null) query.setStartTime(now.minusDays(7));
        if (query.getEndTime() == null) query.setEndTime(now.plusSeconds(1));
        LocalDateTime earliest = now.minusDays(properties.getRetentionDays());
        if (query.getStartTime().isBefore(earliest)) query.setStartTime(earliest);
        if (!query.getStartTime().isBefore(query.getEndTime()) || Duration.between(query.getStartTime(), query.getEndTime()).compareTo(Duration.ofDays(90)) > 0) {
            throw new BusinessException("请选择不超过90天的有效时间范围");
        }
        return PageMethod.startPage(query.getPageNum(), query.getPageSize()).doSelectPageInfo(() -> dao.selectByQO(query));
    }
    /** 按主键查询后再次检查租户归属；平台未归属权限不能读取其他租户日志。 */
    public LoginLogDO selectById(String id) {
        UserContext actor = authorized();
        LoginLogDO row = dao.selectById(id);
        if (row == null || row.getOccurredAt().isBefore(LocalDateTime.now().minusDays(properties.getRetentionDays()))) throw new BusinessException("日志不存在或已过保留期");
        boolean allowed = row.getTenantId() == null ? dao.hasPermission(actor, "loginLog:platform") > 0 : Objects.equals(actor.getTenantId(), row.getTenantId());
        if (!allowed) throw new BusinessException("日志不存在或不可访问");
        return row;
    }
    /** 仅由内部定时任务调用，每批独立提交，避免长事务。 */
    @Transactional(rollbackFor = Exception.class)
    public int deleteExpired() { return dao.deleteExpired(LocalDateTime.now().minusDays(properties.getRetentionDays())); }

    private UserContext authorized() {
        UserContext actor = SecurityContextHolder.get();
        if (actor == null || actor.getUserId() == null || actor.getTenantId() == null || dao.hasPermission(actor, "loginLog:view") == 0) throw new BusinessException("无登录日志查看权限");
        return actor;
    }
}
