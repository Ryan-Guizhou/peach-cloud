package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.userservice.dto.RouterDTO;
import com.peach.userservice.qo.RouterQO;
import com.peach.userservice.vo.RouterVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IRouterService {

    void delById(String id);

    void update(RouterDTO routerDTO);

    void add(RouterDTO routerDTO);

    RouterVO selectById(String id);

    List<RouterVO> list(RouterQO routerQO);

    PageInfo<RouterVO> pageList(RouterQO routerQO);

    boolean uniqueRouterCode(RouterDTO routerDTO);
}
