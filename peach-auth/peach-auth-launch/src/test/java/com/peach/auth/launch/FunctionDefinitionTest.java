package com.peach.auth.launch;

import com.peach.auth.dao.FunctionDao;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.service.impl.FunctionServiceImpl;
import com.peach.auth.vo.FunctionVO;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 功能定义的引用与编码保护回归测试。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 12:00
 */
@ExtendWith(MockitoExtension.class)
class FunctionDefinitionTest {
    @Mock private FunctionDao dao;
    @InjectMocks private FunctionServiceImpl service;

    @BeforeEach void context() {
        UserContext user = new UserContext();
        user.setTenantId("tenant");
        user.setUserId("user");
        SecurityContextHolder.set(user);
    }
    @AfterEach void clear() { SecurityContextHolder.clear(); }

    @Test void rejectsChangingAnExistingCode() {
        FunctionVO existing = new FunctionVO();
        existing.setFuncId("id");
        existing.setFuncCode("ORIGINAL");
        existing.setTenantId("tenant");
        existing.setAppId("app");
        when(dao.selectById("id")).thenReturn(existing);
        FunctionDTO change = new FunctionDTO();
        change.setFuncId("id");
        change.setFuncCode("CHANGED");
        assertThrows(BusinessException.class, () -> service.update(change));
        verify(dao, never()).updateById(any());
    }

    private FunctionVO existing() {
        FunctionVO row = new FunctionVO();
        row.setFuncId("id"); row.setFuncCode("CODE"); row.setTenantId("tenant"); row.setAppId("app");
        when(dao.selectById("id")).thenReturn(row);
        return row;
    }
    @Test void rejectsCrossTenantIds() {
        existing().setTenantId("other");
        assertThrows(BusinessException.class, () -> service.selectById("id"));
    }
    @Test void refusesDeletingReferencedFunctions() {
        FunctionVO row = existing();
        when(dao.lockByCode(row)).thenReturn(row);
        when(dao.countReferences(row)).thenReturn(1);
        assertThrows(BusinessException.class, () -> service.delById("id"));
        verify(dao, never()).delById(any());
    }
    @Test void deletesUnreferencedLeaf() {
        FunctionVO row = existing();
        when(dao.lockByCode(row)).thenReturn(row);
        service.delById("id");
        verify(dao).delById("id");
    }
    @Test void refusesDescendantAsParent() {
        FunctionVO row = existing();
        FunctionVO child = new FunctionVO();
        child.setFuncCode("CHILD"); child.setParentFuncCode("CODE");
        when(dao.lockByCode(any())).thenAnswer(call ->
                "CHILD".equals(((com.peach.auth.entity.FunctionDO) call.getArgument(0)).getFuncCode()) ? child : row);
        FunctionDTO change = new FunctionDTO();
        change.setFuncId("id"); change.setParentFuncCode("CHILD");
        assertThrows(BusinessException.class, () -> service.update(change));
        verify(dao, never()).updateById(any());
    }
    @Test void listAlwaysUsesAuthenticatedTenant() {
        var query = new com.peach.auth.qo.FunctionQO();
        query.setTenantId("forged");
        service.list(query);
        org.junit.jupiter.api.Assertions.assertEquals("tenant", query.getTenantId());
        verify(dao).selectByQO(query);
    }
    @Test void generatesIdAndDefaultsOnCreate() {
        FunctionDTO input = new FunctionDTO();
        input.setFuncCode("NEW"); input.setAppId("app");
        service.add(input);
        var saved = org.mockito.ArgumentCaptor.forClass(com.peach.auth.entity.FunctionDO.class);
        verify(dao).insert(saved.capture());
        org.junit.jupiter.api.Assertions.assertEquals(32, saved.getValue().getFuncId().length());
        org.junit.jupiter.api.Assertions.assertEquals("tenant", saved.getValue().getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals(0, saved.getValue().getIsDelete());
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.getValue().getIsAuthorize());
    }
}
