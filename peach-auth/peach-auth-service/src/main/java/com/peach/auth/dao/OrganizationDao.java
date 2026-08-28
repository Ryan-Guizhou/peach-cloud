package com.peach.auth.dao;

import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.qo.OrganizationQO;
import com.peach.auth.vo.OrganizationVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 机构数据访问。
 * <p>负责机构基础 CRUD 以及带查询条件的列表查询。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Indexed
@MybatisDao
public interface OrganizationDao extends PeachDao<OrganizationDO, OrganizationVO> {

    List<OrganizationVO> selectByQO(OrganizationQO organizationQO);
}
