package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.MenuDTO;
import com.peach.auth.dao.MenuDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.service.IMenuService;
import com.peach.auth.vo.MenuVO;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.HashSet;
import java.util.UUID;
import com.peach.auth.dao.FunctionDao;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.service.support.AdminScope;
import com.peach.code.CodeGenerator;
import com.peach.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;

/**
 * 菜单服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final MenuDao menuDao;
    private final FunctionDao functionDao;
    private final CodeGenerator codeGenerator;

    @Override
    public PageInfo<MenuVO> pageList(MenuQO menuQO) {
        menuQO.setTenantId(AdminScope.actor().getTenantId());
        menuQO.setAppId(AdminScope.APP_ID); menuQO.setIsDelete(0);
        return PageMethod.startPage(menuQO.getPageNum(), menuQO.getPageSize())
                .doSelectPageInfo(() -> menuDao.selectByQO(menuQO));
    }

    @Override
    public MenuVO selectById(String id) {
        var row = menuDao.selectById(id);
        if (row == null || Integer.valueOf(1).equals(row.getIsDelete())
                || !Objects.equals(row.getTenantId(), AdminScope.actor().getTenantId())
                || !AdminScope.APP_ID.equals(row.getAppId())) throw new BusinessException("菜单不存在或不可访问");
        return row;
    }

    @Override
    public void add(MenuDTO menuDTO) {
        MenuDO menuDO = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menuDO);
        menuDO.setMenuId(UUID.randomUUID().toString().replace("-", ""));
        menuDO.setTenantId(AdminScope.actor().getTenantId()); menuDO.setAppId(AdminScope.APP_ID); menuDO.setMenuAppId(AdminScope.APP_ID);
        menuDO.setIsDelete(0);
        if (menuDO.getIsLeaf() == null) menuDO.setIsLeaf(1);
        if (menuDO.getIsDisable() == null) menuDO.setIsDisable(0);
        if (menuDO.getIsShow() == null) menuDO.setIsShow(1);
        validateRelations(menuDO);
        menuDO.setMenuCode(codeGenerator.next(menuDO.getTenantId(), "MENU"));
        menuDO.fillCreateTime(AdminScope.actor().getUserId());
        try { menuDao.insert(menuDO); } catch (DuplicateKeyException ex) { throw new BusinessException("菜单编码或ID已存在"); }
    }

    @Override
    public void delById(String id) {
        selectById(id); menuDao.delById(id);
    }

    @Override
    public void update(MenuDTO menuDTO) {
        var existing = selectById(menuDTO.getMenuId());
        if (menuDTO.getMenuCode() != null && !Objects.equals(menuDTO.getMenuCode(), existing.getMenuCode())) throw new BusinessException("菜单编码创建后不可修改");
        var row = new MenuDO(); BeanUtils.copyProperties(menuDTO, row);
        row.setMenuCode(existing.getMenuCode()); row.setTenantId(existing.getTenantId());
        row.setAppId(existing.getAppId()); row.setMenuAppId(existing.getMenuAppId()); row.setIsDelete(existing.getIsDelete());
        if (row.getFuncCode() == null) row.setFuncCode(existing.getFuncCode());
        if (row.getParentMenuId() == null) row.setParentMenuId(existing.getParentMenuId());
        validateRelations(row);
        row.fillModifyTime(AdminScope.actor().getUserId());
        try { menuDao.updateById(row); } catch (DuplicateKeyException ex) { throw new BusinessException("菜单编码或ID已存在"); }
    }

    private void validateRelations(MenuDO row) {
        var visited = new HashSet<String>(); visited.add(row.getMenuId());
        String parent = row.getParentMenuId();
        while (StringUtil.isNotBlank(parent)) {
            if (!visited.add(parent)) throw new BusinessException("上级菜单不能指向自身或下级");
            parent = selectById(parent).getParentMenuId();
        }
        if (StringUtil.isNotBlank(row.getFuncCode())) {
            var query = new FunctionDO(); query.setTenantId(row.getTenantId()); query.setAppId(row.getAppId()); query.setFuncCode(row.getFuncCode()); query.setIsDelete(0);
            if (functionDao.count(query) == 0) throw new BusinessException("关联功能不存在或不属于当前应用");
        }
    }
}
