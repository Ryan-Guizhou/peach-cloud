package com.peach.auth.core;

import com.github.pagehelper.PageInfo;
import com.peach.auth.qo.UserOperLogQO;
import com.peach.auth.vo.UserOperLogVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:54
 */
public interface IUserOperLogService {

    PageInfo<UserOperLogVO> pageList(UserOperLogQO userOperLogQO);

    void insert(UserOperLogVO userOperLogVO);

    void batchInsert(List<UserOperLogVO> userOperLogVOList);
}
