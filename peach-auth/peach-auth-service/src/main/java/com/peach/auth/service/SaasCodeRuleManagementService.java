package com.peach.auth.service;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.SaasCodeRuleDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.auth.vo.SaasCodeRulePageVO;
import com.peach.auth.vo.SaasCodeRuleVO;
import com.peach.code.CodeFormat;
import com.peach.code.CodeRuleMaintenance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 基于当前 SaaS Token 身份维护现有业务编码规则。
 *
 * <p>调用方不能指定租户命名空间或操作者。平台全局规则只允许实际
 * PLATFORM 控制租户中的不可委派 SYSTEM 管理授权访问；普通租户仅能管理
 * 自己永久业务租户 ID 命名空间下已有的 TENANT 规则。
 * 写入先锁定物理租户并复核权限，再由底层规则行锁、版本检查和同事务审计执行。
 * 预览绝不调用发号器，不保证预览编号被预留。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasCodeRuleManagementService {

    private static final String PLATFORM = "PLATFORM";
    private static final String TENANT = "TENANT";
    private static final String STATUS_ENABLE = "ENABLE";
    private static final int MAX_PAGE = 1000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Pattern SAFE_NAMESPACE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,31}");
    private static final Pattern SAFE_PREFIX = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");

    private final TenantSessionService sessionService;
    private final ScopedPermissionService permissionService;
    private final OrganizationHierarchyDao hierarchyDao;
    private final SaasCodeRuleDao codeRuleDao;
    private final CodeRuleMaintenance ruleMaintenance;

    /**
     * 按当前受信租户返回有界规则目录。
     *
     * @param page 页码，最多 1000
     * @param pageSize 每页最多 50 条
     * @return 不暴露来源租户信息的规则分页
     */
    @Transactional(readOnly = true)
    public SaasCodeRulePageVO list(int page, int pageSize) {
        if (page < 1 || page > MAX_PAGE || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid code rule pagination");
        }
        Scope scope = requireScope(false);
        List<SaasCodeRuleVO> rules = codeRuleDao.selectRules(
                scope.namespace(), scope.scopeType(), pageSize + 1, (page - 1) * pageSize);
        if (rules == null || rules.size() > pageSize + 1 || rules.stream().anyMatch(row -> row == null)) {
            throw new SecurityException("Code rule directory is unavailable or inconsistent");
        }
        return new SaasCodeRulePageVO(page, pageSize, rules.size() > pageSize,
                List.copyOf(rules.subList(0, Math.min(pageSize, rules.size()))));
    }

    /**
     * 查询当前命名空间已有的某条规则。
     */
    @Transactional(readOnly = true)
    public SaasCodeRuleVO get(String prefix) {
        Scope scope = requireScope(false);
        return requireRule(scope, normalizePrefix(prefix));
    }

    /**
     * 读取下一候选编号，仅为展示，不实际占用发号水位。
     */
    @Transactional(readOnly = true)
    public String preview(String prefix) {
        Scope scope = requireScope(false);
        SaasCodeRuleVO rule = requireRule(scope, normalizePrefix(prefix));
        if (!STATUS_ENABLE.equals(rule.getStatus()) || rule.getCurrentValue() == null
                || rule.getCurrentValue() < 0 || rule.getMaxCodeWidth() == null) {
            throw new IllegalStateException("Code rule cannot produce a preview");
        }
        CodeFormat format = new CodeFormat(rule.getCodePrefix(),
                rule.getCodeSeparator(), rule.getMaxCodeWidth());
        return format.preview(Math.addExact(rule.getCurrentValue(), 1L));
    }

    /**
     * 在当前租户写锁下启停已有规则，禁止客户端指定实际命名空间。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void changeStatus(String prefix, boolean enabled, long expectedVersion) {
        Scope scope = requireScope(true);
        String code = normalizePrefix(prefix);
        requireVersion(expectedVersion);
        requireRule(scope, code);
        ruleMaintenance.changeStatus(scope.namespace(), code, enabled,
                expectedVersion, scope.actorRefId());
        log.info("Code rule status changed, scope={}, prefix={}", scope.scopeType(), code);
    }

    /**
     * 底层 MySQL 规则服务仅允许尚未发号的规则变更格式，并原子写审计。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void changeFormat(String prefix, String separator, int width, long expectedVersion) {
        Scope scope = requireScope(true);
        String code = normalizePrefix(prefix);
        requireVersion(expectedVersion);
        requireRule(scope, code);
        CodeFormat format = new CodeFormat(code, separator, width);
        ruleMaintenance.changeFormat(scope.namespace(), code, format,
                expectedVersion, scope.actorRefId());
        log.info("Code rule format changed, scope={}, prefix={}", scope.scopeType(), code);
    }

    /**
     * 只允许在真实事务中向前推进已有规则水位，版本冲突必须回滚。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void advanceWatermark(String prefix, long watermark, long expectedVersion) {
        Scope scope = requireScope(true);
        String code = normalizePrefix(prefix);
        requireVersion(expectedVersion);
        if (watermark < 0) {
            throw new IllegalArgumentException("Code watermark cannot be negative");
        }
        requireRule(scope, code);
        ruleMaintenance.advanceWatermark(scope.namespace(), code, watermark,
                expectedVersion, scope.actorRefId());
        log.info("Code rule watermark advanced, scope={}, prefix={}", scope.scopeType(), code);
    }

    private Scope requireScope(boolean write) {
        TenantSessionContext context = sessionService.requireCurrent();
        if (context == null) {
            throw new SecurityException("Trusted SaaS session is required");
        }
        String tenantBusinessId = write
                ? hierarchyDao.lockTenantAndFindBusinessId(context.tenantRefId())
                : hierarchyDao.findEnabledTenantBusinessId(context.tenantRefId());
        if (!StringUtils.hasText(tenantBusinessId)) {
            throw new SecurityException("Enabled tenant is required for code rule management");
        }
        boolean platformControl = context.tenantRefId().equals(
                codeRuleDao.findPlatformControlTenant(context.tenantRefId()));
        if (platformControl) {
            if (codeRuleDao.countProtectedPlatformManagerGrant(
                    context.tenantRefId(), context.memberRefId(), context.appRefId(),
                    context.memberVersion(), context.authVersion()) < 1) {
                throw new SecurityException("Protected platform code rule permission is required");
            }
            return new Scope(PLATFORM, PLATFORM, context.memberRefId());
        }
        if (PLATFORM.equals(tenantBusinessId) || !SAFE_NAMESPACE.matcher(tenantBusinessId).matches()
                || !permissionService.hasTenantPermission(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                IamPermissionCodes.TENANT_CODE_RULE_MANAGE)) {
            throw new SecurityException("Tenant code rule management permission is required");
        }
        return new Scope(tenantBusinessId, TENANT, context.memberRefId());
    }

    private SaasCodeRuleVO requireRule(Scope scope, String prefix) {
        SaasCodeRuleVO rule = codeRuleDao.selectRule(scope.namespace(), scope.scopeType(), prefix);
        if (rule == null || !prefix.equals(rule.getCodePrefix())
                || !scope.scopeType().equals(rule.getScopeType())
                || rule.getRuleVersion() == null || rule.getRuleVersion() < 0) {
            throw new SecurityException("Code rule is missing or outside the trusted scope");
        }
        return rule;
    }

    private static String normalizePrefix(String input) {
        if (input == null || !SAFE_PREFIX.matcher(input).matches()) {
            throw new IllegalArgumentException("Invalid existing code rule prefix");
        }
        return input.toUpperCase(Locale.ENGLISH);
    }

    private static void requireVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("Code rule version cannot be negative");
        }
    }

    private record Scope(String namespace, String scopeType, String actorRefId) {
    }
}
