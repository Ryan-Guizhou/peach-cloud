package com.peach.log.core;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.BeanUtils;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.common.exception.BusinessException;

import com.peach.log.dao.UserOperLogDao;
import com.peach.log.entity.UserOperLogDO;
import com.peach.log.qo.UserOperLogQO;
import com.peach.log.vo.UserOperLogVO;
import com.peach.common.util.PeachCollectionUtil;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户操作日志服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:59
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UserOperLogServiceImpl implements IUserOperLogService {


    private final UserOperLogDao userOperLogDao;

    private final SqlSessionFactory sqlSessionFactory;

    @Override
    public PageInfo<UserOperLogVO> pageList(UserOperLogQO userOperLogQO) {
        return PageMethod.startPage(userOperLogQO.getPageNum(), userOperLogQO.getPageSize())
                .doSelectPageInfo(() -> userOperLogDao.select(buildQuery(userOperLogQO)));
    }

    @Override
    public void insert(UserOperLogVO userOperLogVO) {
        UserOperLogDO userOperLogDO = copy(userOperLogVO);
        userOperLogDao.insert(userOperLogDO);
    }

    @Override
    public void batchInsert(List<UserOperLogVO> userOperLogVOList) {
        if (PeachCollectionUtil.isEmpty(userOperLogVOList)) {
            return;
        }

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            UserOperLogDao mapper = sqlSession.getMapper(UserOperLogDao.class);

            for (UserOperLogVO vo : userOperLogVOList) {
                UserOperLogDO entity = copy(vo);
                mapper.insert(entity);
            }

            sqlSession.commit();
        } catch (Exception e) {
            log.error("Batch insertion of user operation logs failed, errorType={}", e.getClass().getSimpleName());
            throw e; // 让上层感知失败
        }
    }

    private UserOperLogDO buildQuery(UserOperLogQO userOperLogQO) {
        UserOperLogDO userOperLogDO = new UserOperLogDO();
        var actor = SecurityContextHolder.get();
        if (actor == null || actor.getTenantId() == null || actor.getOrgId() == null) throw new BusinessException("登录上下文缺失");
        userOperLogDO.setTenantId(actor.getTenantId()); userOperLogDO.setOrgId(actor.getOrgId());
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
    private static UserOperLogDO copy(UserOperLogVO source) {
        UserOperLogDO result = new UserOperLogDO(); BeanUtils.copyProperties(source, result); return result;
    }
}
