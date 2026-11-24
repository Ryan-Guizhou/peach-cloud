package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;
import com.peach.code.CodeGenerator;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.dao.FunctionDao;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.qo.FunctionQO;
import com.peach.auth.service.IFunctionService;
import com.peach.auth.vo.FunctionVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.peach.auth.service.support.DefinitionScope;
import com.peach.common.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

/**
 * 功能服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements IFunctionService {

    private final FunctionDao functionDao;
    private final CodeGenerator codeGenerator;

    @Override
    public PageInfo<FunctionVO> pageList(FunctionQO functionQO) {
        functionQO.setTenantId(DefinitionScope.tenantId());
        return PageMethod.startPage(functionQO.getPageNum(), functionQO.getPageSize())
                .doSelectPageInfo(() -> functionDao.selectByQO(functionQO));
    }

    @Override
    public List<FunctionVO> list(FunctionQO functionQO) {
        functionQO.setTenantId(DefinitionScope.tenantId());
        return functionDao.selectByQO(functionQO);
    }

    @Override
    public FunctionVO selectById(String id) {
        FunctionVO row = functionDao.selectById(id);
        if (row == null || Integer.valueOf(1).equals(row.getIsDelete())) throw new BusinessException("功能不存在");
        DefinitionScope.requireTenant(row.getTenantId());
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(FunctionDTO functionDTO) {
        FunctionDO functionDO = new FunctionDO();
        BeanUtils.copyProperties(functionDTO, functionDO);
        functionDO.setFuncId(UUID.randomUUID().toString().replace("-", ""));
        functionDO.setTenantId(DefinitionScope.tenantId());
        if (StringUtil.isBlank(functionDO.getAppId())) throw new BusinessException("应用ID不能为空");
        functionDO.setFuncCode(null);
        validateParent(functionDO);
        functionDO.setFuncCode(codeGenerator.next(functionDO.getTenantId(), "FUNCTION"));
        functionDO.fillCreateTime(null);
        functionDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        if (functionDO.getIsMenu() == null) functionDO.setIsMenu(0);
        if (functionDO.getIsAuthorize() == null) functionDO.setIsAuthorize(1);
        if (functionDO.getIsDisable() == null) functionDO.setIsDisable(0);
        try { functionDao.insert(functionDO); }
        catch (DuplicateKeyException ex) { throw new BusinessException("功能编码或ID已存在"); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delById(String id) {
        FunctionVO row = selectById(id);
        if (functionDao.lockByCode(row) == null) throw new BusinessException("功能已删除，请刷新");
        if (functionDao.countReferences(row) > 0) {
            throw new BusinessException("功能存在下级、资源、菜单、路由或授权引用，请先解除关联");
        }
        functionDao.delById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FunctionDTO functionDTO) {
        FunctionVO existing = selectById(functionDTO.getFuncId());
        if (functionDTO.getFuncCode() != null && !Objects.equals(existing.getFuncCode(), functionDTO.getFuncCode())) {
            throw new BusinessException("功能编码创建后不可修改");
        }
        if (functionDTO.getAppId() != null && !Objects.equals(existing.getAppId(), functionDTO.getAppId())) {
            throw new BusinessException("所属应用创建后不可修改");
        }
        if (functionDao.lockByCode(existing) == null) throw new BusinessException("功能已删除，请刷新");
        FunctionDO change = new FunctionDO();
        BeanUtils.copyProperties(functionDTO, change);
        change.setIsDelete(existing.getIsDelete());
        change.setTenantId(existing.getTenantId());
        change.setAppId(existing.getAppId());
        change.setFuncCode(existing.getFuncCode());
        if (change.getParentFuncCode() == null) change.setParentFuncCode(existing.getParentFuncCode());
        validateParent(change);
        change.fillModifyTime(null);
        functionDao.updateById(change);
    }

    private void validateParent(FunctionDO row) {
        String parent = row.getParentFuncCode();
        Set<String> visited = new HashSet<>();
        visited.add(row.getFuncCode());
        while (StringUtil.isNotBlank(parent)) {
            if (!visited.add(parent)) throw new BusinessException("上级功能不能指向自身或下级，历史循环关系需先修复");
            FunctionDO query = new FunctionDO();
            query.setTenantId(row.getTenantId());
            query.setAppId(row.getAppId());
            query.setFuncCode(parent);
            FunctionVO ancestor = functionDao.lockByCode(query);
            if (ancestor == null) throw new BusinessException("上级功能不存在或不属于当前应用");
            parent = ancestor.getParentFuncCode();
        }
    }
}
