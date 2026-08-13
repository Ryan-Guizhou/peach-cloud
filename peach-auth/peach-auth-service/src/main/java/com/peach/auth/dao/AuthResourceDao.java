package com.peach.auth.dao;

import com.peach.auth.entity.AuthResourceDO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.vo.AuthResourceVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:47
 */
@Indexed
@MybatisDao
public interface AuthResourceDao extends PeachDao<AuthResourceDO, AuthResourceVO> {
}
