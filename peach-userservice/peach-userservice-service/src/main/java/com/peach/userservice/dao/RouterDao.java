package com.peach.userservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.RouterDO;
import com.peach.userservice.qo.RouterQO;
import com.peach.userservice.vo.RouterVO;
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
