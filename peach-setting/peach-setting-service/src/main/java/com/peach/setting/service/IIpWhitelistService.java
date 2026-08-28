package com.peach.setting.service;

import com.peach.common.PageResult;
import com.peach.setting.dto.IpWhitelistDTO;
import com.peach.setting.qo.IpWhitelistQO;
import com.peach.setting.vo.IpWhitelistVO;

import java.util.List;

/**
 * IIpWhitelist服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
public interface IIpWhitelistService {

    /**
     * 分页查询 IP 白名单。
     *
     * @param qo IP 白名单查询参数对象
     * @return IP 白名单分页结果对象
     */
    PageResult<IpWhitelistVO> pageList(IpWhitelistQO qo);

    /**
     * 根据 ID 查询 IP 白名单详情。
     *
     * @param id IP 白名单 ID
     * @return IP 白名单视图对象
     */
    IpWhitelistVO selectById(String id);

    /**
     * 新增 IP 白名单。
     *
     * @param data IP 白名单数据对象
     */
    void save(IpWhitelistDTO data);

    /**
     * 修改 IP 白名单。
     *
     * @param data IP 白名单数据对象
     */
    void update(IpWhitelistDTO data);

    /**
     * 批量删除 IP 白名单。
     *
     * @param ids IP 白名单 ID 列表
     */
    void delete(List<String> ids);

    /**
     * 预热 IP 白名单缓存。
     */
    void warmUpCache();
}
