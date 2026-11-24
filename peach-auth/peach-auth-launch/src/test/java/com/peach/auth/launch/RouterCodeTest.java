package com.peach.auth.launch;

import com.peach.auth.dao.FunctionDao;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.dto.RouterDTO;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.service.impl.RouterServiceImpl;
import com.peach.auth.service.support.AdminScope;
import com.peach.auth.vo.RouterVO;
import com.peach.code.CodeGenerator;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * 验证服务端路由发号与历史编码不可变。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 18:00
 */
class RouterCodeTest {
    @AfterEach void clear() { SecurityContextHolder.clear(); }
    @Test void allocationIgnoresClientCodeAndUpdateRetainsHistory() {
        var actor = new UserContext(); actor.setTenantId("tenant");actor.setOrgId("org");actor.setUserId("user");actor.setFiscal("2026");SecurityContextHolder.set(actor);
        var dao=mock(RouterDao.class);var functions=mock(FunctionDao.class);var generator=mock(CodeGenerator.class);
        when(generator.next("tenant","ROUTER")).thenReturn("RT0001");
        var service=new RouterServiceImpl(dao,functions,generator);
        var input=new RouterDTO();input.setRouterCode("client-code");input.setIsAuth(0);
        service.add(input);
        verify(dao).insert(argThat((RouterDO row)->"RT0001".equals(row.getRouterCode())));
        var row=new RouterVO();row.setTenantId("tenant");row.setAppId(AdminScope.APP_ID);row.setRouterCode("SYS_EXISTING");row.setIsAuth(0);
        when(dao.selectById("id")).thenReturn(row);
        input.setRouterId("id");input.setRouterCode(null);service.update(input);
        assertEquals("SYS_EXISTING",row.getRouterCode());
        input.setRouterCode("changed");assertThrows(BusinessException.class,()->service.update(input));
        verify(generator,times(1)).next(anyString(),anyString());
    }
}
