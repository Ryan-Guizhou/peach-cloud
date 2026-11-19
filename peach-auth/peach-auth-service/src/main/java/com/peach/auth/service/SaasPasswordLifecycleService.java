package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.SaasPasswordChangeDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.PasswordCipherService;
import com.peach.auth.service.support.PasswordHashSupport;
import com.peach.auth.service.support.UserTokenRevoker;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.common.unique.UniqueIdFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 已认证 SaaS 全局账号的密码修改、版本失效与事务审计。
 *
 * <p>账号来源只能是实时核验的 Token，用户物理行使用 FOR UPDATE 串行化改密。
 * 密码散列、用户级凭证版本递增和脱敏审计同事务提交；
 * 提交后尽力回收全账号 Token。即使 Redis 回收失败，旧 Token 由于
 * PASSWORD_VERSION 不匹配也不能再通过 SaaS 在线校验。
 * 不提供未经真实身份核实的密码找回或匿名重置后门。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasPasswordLifecycleService {

    private static final String LOCK_KEY_PREFIX = "password-change:";
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BYTES = 72;

    private final TenantSessionService sessionService;
    private final UserDao userDao;
    private final LoginLockService loginLockService;
    private final PasswordCipherService passwordCipherService;
    private final PasswordHashSupport passwordHashSupport;
    private final UserTokenRevoker tokenRevoker;
    private final IamAuthorizationAuditDao auditDao;

    /**
     * 原密码校验成功后修改全局账号密码，提交后回收所有设备和租户下的旧 Token。
     *
     * @param request RSA 加密的旧/新密码，禁止直接打印或序列化返回
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void changePassword(SaasPasswordChangeDTO request) {
        if (request == null || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new IllegalArgumentException("Password change credentials are required");
        }
        TenantSessionContext context = sessionService.requireCurrent();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Password change requires a managed transaction");
        }
        String failureKey = LOCK_KEY_PREFIX + context.userRefId();
        LoginLockStatusVO lock = loginLockService.checkLock(failureKey);
        if (lock == null || lock.isLocked()) {
            throw new SecurityException("Password change is temporarily unavailable");
        }

        String previousPassword;
        String nextPassword;
        try {
            previousPassword = passwordCipherService.decrypt(request.getOldPassword());
            nextPassword = passwordCipherService.decrypt(request.getNewPassword());
        } catch (SecurityException exception) {
            loginLockService.recordFailure(failureKey);
            throw new SecurityException("Password verification failed");
        }

        UserDO current = userDao.lockCredential(context.userRefId());
        if (current == null || !context.userRefId().equals(current.getId())
                || !StringUtils.hasText(current.getUserId())
                || current.getPasswordVersion() == null || current.getPasswordVersion() < 0
                || current.getPasswordVersion() == Long.MAX_VALUE) {
            throw new SecurityException("Active password credential is unavailable");
        }
        if (!passwordHashSupport.matches(previousPassword, current.getPassword())) {
            loginLockService.recordFailure(failureKey);
            throw new SecurityException("Password verification failed");
        }
        requireStrongPassword(nextPassword);
        if (passwordHashSupport.matches(nextPassword, current.getPassword())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }
        long oldVersion = current.getPasswordVersion();
        String passwordHash = passwordHashSupport.encode(nextPassword);
        if (userDao.changePassword(context.userRefId(), passwordHash,
                oldVersion, context.memberRefId()) != 1) {
            throw new IllegalStateException("Concurrent password change detected");
        }
        if (auditDao.append(UniqueIdFacade.nextId(), context.tenantRefId(),
                context.appRefId(), context.memberRefId(), context.userRefId(),
                "USER_PASSWORD_CHANGE", oldVersion, oldVersion + 1) != 1) {
            throw new IllegalStateException("Password change audit failed");
        }
        loginLockService.clearOnSuccess(failureKey);
        String businessUserId = current.getUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    tokenRevoker.revokeAll(businessUserId);
                } catch (RuntimeException exception) {
                    // 旧 Token 仍会因数据库密码版本不一致被拒绝。
                    log.error("Post-commit global token revocation failed");
                }
            }
        });
        log.info("Global account password changed, userRefId={}, newPasswordVersion={}",
                context.userRefId(), oldVersion + 1);
    }

    /**
     * 采用可验证、明确的 ASCII 密码策略，避免 BCrypt 72 字节截断重号。
     *
     * <p>至少 12 位，包含大写、小写、数字及可打印符号，禁止空格和控制字符。</p>
     */
    static void requireStrongPassword(String candidate) {
        if (candidate == null || candidate.length() < MIN_PASSWORD_LENGTH
                || candidate.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("New password does not meet security requirements");
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean symbol = false;
        for (int index = 0; index < candidate.length(); index++) {
            char ch = candidate.charAt(index);
            if (ch < '!' || ch > '~') {
                throw new IllegalArgumentException("New password contains unsupported characters");
            }
            upper |= ch >= 'A' && ch <= 'Z';
            lower |= ch >= 'a' && ch <= 'z';
            digit |= ch >= '0' && ch <= '9';
            symbol |= !Character.isLetterOrDigit(ch);
        }
        if (!upper || !lower || !digit || !symbol) {
            throw new IllegalArgumentException("New password does not meet security requirements");
        }
    }
}
