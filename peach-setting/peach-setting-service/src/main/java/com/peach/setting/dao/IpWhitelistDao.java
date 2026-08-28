package com.peach.setting.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.setting.entity.IpWhitelistDO;
import com.peach.setting.qo.IpWhitelistQO;
import com.peach.setting.vo.IpWhitelistVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * IpWhitelist数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Indexed
@MybatisDao
public interface IpWhitelistDao extends PeachDao<IpWhitelistDO, IpWhitelistVO> {

    /**
     * 根据查询参数对象查询 IP 白名单列表。
     *
     * @param qo IP 白名单查询参数对象
     * @return IP 白名单视图对象列表
     */
    List<IpWhitelistVO> selectByQO(IpWhitelistQO qo);

    /**
     * 查询启用的 IP 白名单列表。
     *
     * @return 启用的 IP 白名单视图对象列表
     */
    List<IpWhitelistVO> selectEnabledList();
}
