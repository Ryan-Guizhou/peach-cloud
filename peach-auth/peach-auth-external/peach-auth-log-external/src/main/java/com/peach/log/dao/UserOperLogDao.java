package com.peach.log.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.log.entity.UserOperLogDO;
import com.peach.log.vo.UserOperLogVO;
import org.springframework.stereotype.Indexed;

/**
 * 用户操作日志数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:51
 */
@Indexed
@MybatisDao
public interface UserOperLogDao extends PeachDao<UserOperLogDO, UserOperLogVO> {

}
