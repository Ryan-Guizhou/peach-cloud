package com.peach.auth.dao;

import com.peach.auth.qo.ResourceQO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.ResourceDO;
import com.peach.auth.vo.ResourceVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 资源数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:44
 */
@Indexed
@MybatisDao
public interface ResourceDao extends PeachDao<ResourceDO, ResourceVO> {

    List<ResourceVO> selectByQO(ResourceQO resourceQO);
}
