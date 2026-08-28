package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.vo.AuthLogVO;
import org.springframework.stereotype.Indexed;

/**
 * 认证日志数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:47
 */
@Indexed
@MybatisDao
public interface AuthLogDao extends PeachDao<AuthLogDO, AuthLogVO> {
}
