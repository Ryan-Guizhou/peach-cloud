package com.peach.auth.service;

import com.peach.auth.dto.UserRoleAuthDTO;
import com.peach.auth.qo.AuthPartyQO;
import com.peach.auth.vo.AuthPartyVO;

import java.util.List;

/**
 * 用户角色绑定服务。
 */
public interface IAuthPartyService {

    List<AuthPartyVO> listUserRoles(AuthPartyQO authPartyQO);

    void saveUserRoles(UserRoleAuthDTO userRoleAuthDTO);
}
