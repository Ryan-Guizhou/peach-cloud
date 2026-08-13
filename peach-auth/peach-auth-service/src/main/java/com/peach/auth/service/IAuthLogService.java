package com.peach.auth.service;

import com.github.pagehelper.PageInfo;
import com.peach.auth.entity.AuthLogDO;
import com.peach.auth.qo.AuthLogQO;
import com.peach.auth.vo.AuthLogVO;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:25
 */
public interface IAuthLogService {

    PageInfo<AuthLogVO> pageList(AuthLogQO authLogQO);

    void record(AuthLogDO authLogDO);
}
