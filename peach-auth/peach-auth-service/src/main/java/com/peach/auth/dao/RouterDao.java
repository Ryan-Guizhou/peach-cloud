package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.qo.RouterQO;
import com.peach.auth.vo.RouterVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:45
 */
@Indexed
@MybatisDao
public interface RouterDao extends PeachDao<RouterDO, RouterVO> {

    List<RouterVO> selectByQO(RouterQO routerQO);

    int countByRouterCode(RouterDO routerDO);
}
