package com.peach.userservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.FunctionDO;
import com.peach.userservice.vo.FunctionVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:46
 */
@Indexed
@MybatisDao
public interface FunctionDao extends PeachDao<FunctionDO, FunctionVO> {
}
