package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.vo.FunctionVO;
import org.springframework.stereotype.Indexed;

/**
 * 功能数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:46
 */
@Indexed
@MybatisDao
public interface FunctionDao extends PeachDao<FunctionDO, FunctionVO> {
    /** 按租户、应用及功能条件查询，返回稳定排序的完整树数据。 */
    java.util.List<FunctionVO> selectByQO(com.peach.auth.qo.FunctionQO query);

    /** 锁定功能定义；调用方必须在事务内使用，同一功能的资源维护共享此锁。 */
    FunctionVO lockByCode(FunctionDO query);

    /** 统计同租户、同应用下的下级、资源、菜单、路由及授权引用。 */
    int countReferences(FunctionDO query);
}
