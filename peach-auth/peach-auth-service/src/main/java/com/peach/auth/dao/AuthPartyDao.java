package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.AuthPartyDO;
import com.peach.auth.vo.AuthPartyVO;
import org.springframework.stereotype.Indexed;

/**
 * 认证参与方数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:48
 */
@Indexed
@MybatisDao
public interface AuthPartyDao extends PeachDao<AuthPartyDO, AuthPartyVO> {

    /**
     * 查询绑定指定角色的用户账号列表。
     */
    java.util.List<String> selectUserCodesByRole(AuthPartyDO query);
}
