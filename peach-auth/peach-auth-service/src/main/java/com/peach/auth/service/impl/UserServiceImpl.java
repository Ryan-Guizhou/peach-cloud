package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.LoginInfo;
import com.peach.auth.common.RsaPasswordUtil;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IMenuService;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.IUserService;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.UserVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    @Resource
    private IMenuService iMenuService;

    @Resource
    private IRoleService iRoleService;

    @Override
    public PageInfo<UserVO> pageList(UserQO userQO) {
        PageInfo<UserVO> pageInfo = PageHelper.startPage(userQO.getPageNum(), userQO.getPageSize())
                .doSelectPageInfo(() -> userDao.selectByQO(userQO));
        return pageInfo;
    }

    @Override
    public List<UserVO> list(UserQO userQO) {
        if (userQO != null && !CollectionUtils.isEmpty(userQO.getUserIdList())) {
            return userDao.selectByIds(userQO.getUserIdList());
        }
        return userDao.select(buildUserDO(userQO));
    }

    @Override
    public Response login(LoginDTO loginDTO) {
        String password = loginDTO.getPassword();
        String username = loginDTO.getUsername();
        String decryptPassword = null;
        try {
            decryptPassword = RsaPasswordUtil.decrypt(password);
        } catch (Exception e) {
            log.error("解密密码失败:" + e.getMessage(), e);
        }
        if (StringUtil.isBlank(decryptPassword)) {
            return Response.fail("密码解密失败");
        }
        String base64Password = Base64.getEncoder().encodeToString(decryptPassword.getBytes());
        UserVO userVO = userDao.login(username, base64Password);
        if (ObjectUtil.isNull(userVO)) {
            log.error("用户名或密码错误,username:{},base64Password:{}", username, base64Password);
            return Response.fail("用户名或密码错误");
        }
        String userId = userVO.getUserId();
        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        log.info("用户:{}，登录成功，token:{}", username, token);

        // 角色
        RoleQO roleQO = new RoleQO();
        roleQO.setFiscal(loginDTO.getFiscal());
        roleQO.setUserCode(userVO.getUserCode());
        List<RoleVO> roleList = iRoleService.selectByUserCode(roleQO);


        // 菜单


        // 获取权限
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setUserId(userVO.getUserId());
        loginInfo.setUserName(userVO.getUserName());
        loginInfo.setRoleList(roleList);
        loginInfo.setToken(token);
        return Response.success(loginInfo);
    }

    private Map roleAndMenu(List<RoleVO> roleVOList, Integer fiscal) {
        return new HashMap();
    }

    @Override
    public Response register(RegisterDTO registerDTO) {
        // TODO: 实现注册逻辑
        return Response.success();
    }

    @Override
    public UserVO selectUserById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new UserVO();
        }
        return userDao.selectById(id);
    }

    @Override
    public void add(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userDTO, userDO);
        userDO.fillCreateTime(null);
        if (userDO.getIsDelete() == null) {
            userDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        userDao.insert(userDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        userDao.delById(id);
    }

    @Override
    public void update(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userDTO, userDO);
        userDO.fillModifyTime(null);
        userDao.updateById(userDO);
    }

    private UserDO buildUserDO(UserQO userQO) {
        UserDO userDO = new UserDO();
        if (userQO == null) {
            return userDO;
        }
        userDO.setUserId(userQO.getUserId());
        userDO.setUserName(userQO.getUsername());
        userDO.setMobilePhone(userQO.getPhone());
        return userDO;
    }
}
