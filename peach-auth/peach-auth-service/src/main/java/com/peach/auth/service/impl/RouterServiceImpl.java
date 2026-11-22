package com.peach.auth.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.dao.FunctionDao;
import com.peach.auth.dto.RouterDTO;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.qo.RouterQO;
import com.peach.auth.service.IRouterService;
import com.peach.auth.service.support.AdminScope;
import com.peach.auth.vo.RouterVO;
import com.peach.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 当前租户应用的路由查询和维护，按主键更新并验证功能归属。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Service
@RequiredArgsConstructor
public class RouterServiceImpl implements IRouterService {
    private final RouterDao routerDao;
    private final FunctionDao functionDao;
    @Override public PageInfo<RouterVO> pageList(RouterQO query) {
        scope(query);
        return PageMethod.startPage(query.getPageNum(), query.getPageSize()).doSelectPageInfo(() -> routerDao.selectByQO(query));
    }
    @Override public List<RouterVO> list(RouterQO query) { scope(query); return routerDao.selectByQO(query); }
    private void scope(RouterQO query) { query.setTenantId(AdminScope.actor().getTenantId()); query.setAppId(AdminScope.APP_ID); }
    @Override public RouterVO selectById(String id) {
        var row = routerDao.selectById(id);
        if (row == null || !Objects.equals(row.getTenantId(), AdminScope.actor().getTenantId()) || !AdminScope.APP_ID.equals(row.getAppId())) throw new BusinessException("路由不存在或不可访问");
        return row;
    }
    @Override public void add(RouterDTO input) {
        RouterDO row = new RouterDO(); BeanUtils.copyProperties(input, row);
        row.setRouterId(UUID.randomUUID().toString().replace("-", "")); row.setTenantId(AdminScope.actor().getTenantId()); row.setAppId(AdminScope.APP_ID);
        if (row.getIsAuth() == null) row.setIsAuth(1);
        if (row.getIsCache() == null) row.setIsCache(0);
        if (row.getRouterLevel() == null) row.setRouterLevel(2);
        validateFunction(row); row.fillCreateTime(AdminScope.actor().getUserId());
        try { routerDao.insert(row); } catch (DuplicateKeyException ex) { throw new BusinessException("路由编码或URL已存在"); }
    }
    @Override public void update(RouterDTO input) {
        RouterVO row = selectById(input.getRouterId());
        String originalFunction = row.getFuncCode();
        BeanUtils.copyProperties(input, row, "isAuth", "isCache");
        if (input.getFuncCode() == null) row.setFuncCode(originalFunction);
        // Hidden flags keep their stored values unless an existing API client explicitly supplies them.
        if (input.getIsAuth() != null) row.setIsAuth(input.getIsAuth());
        if (input.getIsCache() != null) row.setIsCache(input.getIsCache());
        row.setTenantId(AdminScope.actor().getTenantId()); row.setAppId(AdminScope.APP_ID);
        validateFunction(row); row.fillModifyTime(AdminScope.actor().getUserId());
        // Omitted hidden flags must not be written back from an earlier read.
        row.setIsAuth(input.getIsAuth()); row.setIsCache(input.getIsCache());
        try { routerDao.updateById(row); } catch (DuplicateKeyException ex) { throw new BusinessException("路由编码或URL已存在"); }
    }
    @Override public void delById(String id) { selectById(id); routerDao.delById(id); }
    @Override public boolean uniqueRouterCode(RouterDTO input) {
        RouterDO query = new RouterDO(); query.setRouterId(input.getRouterId()); query.setRouterCode(input.getRouterCode());
        query.setTenantId(AdminScope.actor().getTenantId()); query.setAppId(AdminScope.APP_ID);
        return routerDao.countByRouterCode(query) > 0;
    }
    private void validateFunction(RouterDO row) {
        if (row.getFuncCode() == null || row.getFuncCode().isBlank()) {
            if (Integer.valueOf(1).equals(row.getIsAuth())) throw new BusinessException("需要授权的路由必须关联功能");
            return;
        }
        FunctionDO query = new FunctionDO(); query.setTenantId(row.getTenantId()); query.setAppId(row.getAppId()); query.setFuncCode(row.getFuncCode()); query.setIsDelete(0);
        if (functionDao.count(query) == 0) throw new BusinessException("关联功能不存在或不属于当前应用");
    }
}
