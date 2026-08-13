package com.peach.auth.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.peach.auth.dao.UserOperLogDao;
import com.peach.auth.entity.UserOperLogDO;
import com.peach.auth.qo.UserOperLogQO;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.util.PeachCollectionUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:59
 */
@Slf4j
@Indexed
@Service
public class UserOperLogServiceImpl implements IUserOperLogService {

    private static final ObjectMapper _mapper = new ObjectMapper();

    @Resource
    private UserOperLogDao userOperLogDao;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public PageInfo<UserOperLogVO> pageList(UserOperLogQO userOperLogQO) {
        return PageHelper.startPage(userOperLogQO.getPageNum(), userOperLogQO.getPageSize())
                .doSelectPageInfo(() -> userOperLogDao.select(buildQuery(userOperLogQO)));
    }

    @Override
    public void insert(UserOperLogVO userOperLogVO) {
        UserOperLogDO userOperLogDO = _mapper.convertValue(userOperLogVO, UserOperLogDO.class);
        userOperLogDao.insert(userOperLogDO);
    }

    @Override
    public void batchInsert(List<UserOperLogVO> userOperLogVOList) {
        if (PeachCollectionUtil.isEmpty(userOperLogVOList)) {
            return;
        }

        try (SqlSession sqlSession =
                     sqlSessionFactory.openSession(ExecutorType.BATCH)) {

            UserOperLogDao mapper = sqlSession.getMapper(UserOperLogDao.class);

            for (UserOperLogVO vo : userOperLogVOList) {
                UserOperLogDO entity =
                        _mapper.convertValue(vo, UserOperLogDO.class);
                mapper.insert(entity);
            }

            sqlSession.commit();
        } catch (Exception e) {
            log.error("Batch insertion of user operation logs failed", e);
            throw e; // 让上层感知失败
        }
    }

    private UserOperLogDO buildQuery(UserOperLogQO userOperLogQO) {
        UserOperLogDO userOperLogDO = new UserOperLogDO();
        if (userOperLogQO == null) {
            return userOperLogDO;
        }
        userOperLogDO.setOptTypeCode(userOperLogQO.getOptTypeCode());
        userOperLogDO.setModuleCode(userOperLogQO.getModuleCode());
        userOperLogDO.setCreatorCode(userOperLogQO.getCreatorCode());
        userOperLogDO.setOptLevel(userOperLogQO.getOptLevel());
        userOperLogDO.setIsSuccess(userOperLogQO.getIsSuccess());
        userOperLogDO.setRequestUri(userOperLogQO.getRequestUri());
        userOperLogDO.setRequestMethod(userOperLogQO.getRequestMethod());
        return userOperLogDO;
    }
}
