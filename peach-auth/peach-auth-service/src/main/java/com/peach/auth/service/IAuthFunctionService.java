package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RoleFunctionAuthDTO;
import com.peach.auth.qo.AuthFunctionQO;
import com.peach.auth.vo.AuthFunctionVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IAuthFunctionService {

    PageInfo<AuthFunctionVO> pageList(AuthFunctionQO authFunctionQO);

    List<AuthFunctionVO> list(AuthFunctionQO authFunctionQO);

    void saveRoleFunctions(RoleFunctionAuthDTO roleFunctionAuthDTO);
}
