package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.common.response.Response;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.service.IUserService;
import com.peach.auth.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;



/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Slf4j
@Indexed
@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private UserDao userDao;

    @Override
    public Response login(LoginDTO loginDTO) {
        UserDO userDO = userDao.selectById("u000000000000000000001");
        StpUtil.login(userDO.getUserId());
        return Response.success(StpUtil.getTokenInfo());
    }

    @Override
    public UserVO selectUserById(String id) {
        return null;
    }
}
