package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RoleResourceAuthDTO;
import com.peach.auth.qo.AuthResourceQO;
import com.peach.auth.vo.AuthResourceVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IAuthResourceService {

    PageInfo<AuthResourceVO> pageList(AuthResourceQO authResourceQO);

    List<AuthResourceVO> list(AuthResourceQO authResourceQO);

    void saveRoleResources(RoleResourceAuthDTO roleResourceAuthDTO);
}
