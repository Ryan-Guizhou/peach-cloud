package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.ResourceDTO;
import com.peach.auth.dao.ResourceDao;
import com.peach.auth.entity.ResourceDO;
import com.peach.auth.qo.ResourceQO;
import com.peach.auth.service.IResouceService;
import com.peach.auth.vo.ResourceVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import com.peach.auth.dao.FunctionDao;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.service.support.DefinitionScope;
import com.peach.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/**
 * ResouceServce实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:35
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ResouceServceImpl implements IResouceService {

    private final ResourceDao resourceDao;
    private final FunctionDao functionDao;

    @Override
    public PageInfo<ResourceVO> pageList(ResourceQO resourceQO) {
        resourceQO.setTenantId(DefinitionScope.tenantId());
        return PageMethod.startPage(resourceQO.getPageNum(), resourceQO.getPageSize())
                .doSelectPageInfo(() -> resourceDao.selectByQO(resourceQO));
    }

    @Override
    public List<ResourceVO> list(ResourceQO resourceQO) {
        resourceQO.setTenantId(DefinitionScope.tenantId());
        return resourceDao.selectByQO(resourceQO);
    }

    @Override
    public ResourceVO selectById(String id) {
        ResourceVO row = resourceDao.selectById(id);
        if (row == null || Integer.valueOf(1).equals(row.getIsDelete())) throw new BusinessException("资源不存在");
        DefinitionScope.requireTenant(row.getTenantId());
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ResourceDTO resourceDTO) {
        ResourceDO resourceDO = new ResourceDO();
        BeanUtils.copyProperties(resourceDTO, resourceDO);
        if (StringUtil.isBlank(resourceDO.getResourceId())) resourceDO.setResourceId(UUID.randomUUID().toString().replace("-", ""));
        resourceDO.setTenantId(DefinitionScope.tenantId());
        lockFunction(resourceDO);
        resourceDO.fillCreateTime(null);
        resourceDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        try { resourceDao.insert(resourceDO); }
        catch (DuplicateKeyException ex) { throw new BusinessException("当前功能下资源编码或ID已存在"); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delById(String id) {
        ResourceVO row = selectById(id);
        lockFunction(row);
        if (resourceDao.countReferences(row) > 0) throw new BusinessException("资源存在授权引用，请先解除授权");
        resourceDao.delById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ResourceDTO resourceDTO) {
        ResourceVO existing = selectById(resourceDTO.getResourceId());
        lockFunction(existing);
        ResourceDO change = new ResourceDO();
        BeanUtils.copyProperties(resourceDTO, change);
        if ((change.getFuncCode() != null && !Objects.equals(change.getFuncCode(), existing.getFuncCode()))
                || (change.getAppId() != null && !Objects.equals(change.getAppId(), existing.getAppId()))) {
            throw new BusinessException("资源所属功能和应用不可修改");
        }
        if (resourceDao.countReferences(existing) > 0
                && ((change.getResourceCode() != null && !Objects.equals(change.getResourceCode(), existing.getResourceCode()))
                || (change.getResourceType() != null && !Objects.equals(change.getResourceType(), existing.getResourceType())))) {
            throw new BusinessException("已授权资源的编码和类型不可修改，请先解除授权");
        }
        change.setIsDelete(existing.getIsDelete());
        change.setTenantId(existing.getTenantId());
        change.fillModifyTime(null);
        try { resourceDao.updateById(change); }
        catch (DuplicateKeyException ex) { throw new BusinessException("当前功能下资源编码已存在"); }
    }

    private void lockFunction(ResourceDO row) {
        if (StringUtil.isBlank(row.getFuncCode()) || StringUtil.isBlank(row.getAppId())) {
            throw new BusinessException("功能编码和应用ID不能为空");
        }
        FunctionDO query = new FunctionDO();
        query.setTenantId(row.getTenantId());
        query.setAppId(row.getAppId());
        query.setFuncCode(row.getFuncCode());
        if (functionDao.lockByCode(query) == null) throw new BusinessException("所属功能不存在或不可访问");
    }
}
