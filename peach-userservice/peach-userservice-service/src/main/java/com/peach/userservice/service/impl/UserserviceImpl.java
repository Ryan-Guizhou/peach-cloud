package com.peach.userservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.peach.common.util.PeachCollectionUtil;
import com.peach.userservice.dao.UserDao;
import com.peach.userservice.entity.UserDO;
import com.peach.userservice.qo.UserQO;
import com.peach.userservice.service.IUserservice;
import com.peach.userservice.vo.UserVO;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Service
public class UserserviceImpl implements IUserservice {

    @Resource
    private UserDao userDao;

    @Override
    public List<UserVO> list(UserQO userQO) {
        List<UserDO> list = userDao.selectByQO(userQO);
        return PeachCollectionUtil.isEmpty(list) ? Collections.emptyList() :list.stream().map(userDO -> {
            UserVO userVO = new UserVO();
            try {
                BeanUtils.copyProperties(userVO,userDO);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return userVO;
        }).collect(Collectors.toList());
    }

    @Override
    public UserVO getById(String id) {
        UserDO userDO = userDao.selectById(id);
        UserVO userVO = new UserVO();
        if (Optional.ofNullable(userDO).isPresent()) {
            try {
                BeanUtils.copyProperties(userVO,userDO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return userVO;
    }
}
