package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.UserDTO;
import com.peach.common.response.Response;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口。
 *
 * <p>提供用户分页、查询、登录、注册以及基础增删改能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
public interface IUserService {

    /**
     * 分页查询用户列表。
     *
     * @param userQO 查询条件
     * @return 分页结果
     */
    PageInfo<UserVO> pageList(UserQO userQO);

    /**
     * 查询用户列表。
     *
     * @param userQO 查询条件
     * @return 用户列表
     */
    List<UserVO> list(UserQO userQO);

    /**
     * 用户登录。
     *
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    Response login(LoginDTO loginDTO);

    /**
     * 用户注册。
     *
     * @param registerDTO 注册参数
     * @return 注册结果
     */
    Response register(RegisterDTO registerDTO);

    /**
     * 根据用户ID查询用户。
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO selectUserById(String id);

    /**
     * 新增用户。
     *
     * @param userDTO 新增参数
     */
    void add(UserDTO userDTO);

    /**
     * 根据用户ID删除用户。
     *
     * @param id 用户ID
     */
    void delById(String id);

    /**
     * 更新用户。
     *
     * @param userDTO 更新参数
     */
    void update(UserDTO userDTO);

}
