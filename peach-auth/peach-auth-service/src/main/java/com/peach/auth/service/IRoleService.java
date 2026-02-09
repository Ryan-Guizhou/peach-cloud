package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.vo.RoleVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IRoleService {

    PageInfo<RoleVO> pageList(RoleQO roleQO);

    List<RoleVO> list(RoleQO roleQO);

    RoleVO selectById(String id);

    void add(RoleQO roleQO);

    void delById(String id);

    void update(RoleQO roleQO);
}
