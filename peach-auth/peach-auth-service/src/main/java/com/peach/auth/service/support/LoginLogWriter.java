package com.peach.auth.service.support;

import com.peach.auth.dao.LoginLogDao;
import com.peach.auth.entity.LoginLogDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在独立事务中提交认证审计，使登录失败或外层回滚不丢弃已提交记录。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Service
@RequiredArgsConstructor
public class LoginLogWriter {
    private final LoginLogDao dao;
    /** 阻塞写入独立事务；异常交由请求记录器告警，不改变认证结果。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(LoginLogDO row) { dao.insert(row); }
}
