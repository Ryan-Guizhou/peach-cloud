package com.peach.userservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.AuthPartyDO;
import com.peach.userservice.vo.AuthPartyVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:48
 */
@Indexed
@MybatisDao
public interface AuthPartyDao extends PeachDao<AuthPartyDO, AuthPartyVO> {
}
