package com.peach.auth.service;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.TenantDTO;
import com.peach.auth.qo.TenantQO;
import com.peach.auth.vo.TenantVO;
import java.util.List;
/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
public interface ITenantService {
    PageInfo<TenantVO> pageList(TenantQO query);
    List<TenantVO> list(TenantQO query);
    TenantVO selectById(String id);
    void add(TenantDTO input);
    void update(TenantDTO input);
    void status(String id, Long version, String status);
    void delete(String id, Long version);
}
