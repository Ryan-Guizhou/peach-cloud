package com.peach.auth.launch.tenant;
import com.peach.auth.dao.TenantDao;
import com.peach.auth.dto.TenantDTO;
import com.peach.auth.entity.TenantDO;
import com.peach.auth.event.TenantUnavailableEvent;
import com.peach.auth.service.impl.TenantServiceImpl;
import com.peach.auth.vo.TenantVO;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
class TenantServiceImplTest {
    TenantDao dao;
    ApplicationEventPublisher events;
    TenantServiceImpl service;
    @BeforeEach void setup() {
        dao = mock(TenantDao.class);
        events = mock(ApplicationEventPublisher.class);
        service = new TenantServiceImpl(dao, events);
        UserContext actor = new UserContext();
        actor.setUserId("admin-id"); actor.setUserCode("admin-account"); actor.setTenantId("current");
        SecurityContextHolder.set(actor);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clear(); }
    TenantVO tenant(String status) {
        TenantVO tenant = new TenantVO();
        tenant.setId("id"); tenant.setTenantId("other"); tenant.setVersion(2L); tenant.setStatus(status);
        when(dao.selectById("id")).thenReturn(tenant);
        return tenant;
    }
    @Test void creationGeneratesStableIdentifiersAndServerAudit() {
        TenantDTO input = new TenantDTO(); input.setTenantCode("CODE"); input.setTenantName("Name"); input.setTenantType("ENTERPRISE");
        input.setId("client-id"); input.setVersion(999L);
        service.add(input);
        ArgumentCaptor<TenantDO> saved = ArgumentCaptor.forClass(TenantDO.class);
        verify(dao).insert(saved.capture());
        TenantDO value = saved.getValue();
        assertEquals(32, value.getId().length()); assertEquals(32, value.getTenantId().length());
        assertNotEquals(value.getId(), value.getTenantId());
        assertEquals("PENDING", value.getStatus()); assertEquals(0L, value.getVersion());
        assertEquals("admin-account", value.getCreatorCode()); assertNull(value.getActivatedTime());
    }
    @Test void activationTimestampIsPreservedAcrossSuspension() {
        TenantVO tenant = tenant("PENDING"); when(dao.updateStatus(any())).thenReturn(1);
        service.status("id", 2L, "ACTIVE");
        String first = tenant.getActivatedTime(); assertNotNull(first);
        service.status("id", 2L, "SUSPENDED");
        service.status("id", 2L, "ACTIVE");
        assertEquals(first, tenant.getActivatedTime());
        verify(events).publishEvent(any(TenantUnavailableEvent.class));
    }
    @Test void closedTenantIsTerminal() {
        tenant("CLOSED");
        assertThrows(BusinessException.class, () -> service.status("id", 2L, "ACTIVE"));
        verify(dao, never()).updateStatus(any());
    }
    @Test void currentTenantCannotBeSuspendedOrDeleted() {
        TenantVO tenant = tenant("ACTIVE"); tenant.setTenantId("current");
        assertThrows(BusinessException.class, () -> service.status("id", 2L, "SUSPENDED"));
        tenant.setStatus("CLOSED");
        assertThrows(BusinessException.class, () -> service.delete("id", 2L));
        verify(dao, never()).updateStatus(any()); verify(dao, never()).logicalDelete(any());
    }
    @Test void staleDatabaseVersionRejectsStatusAndPublishesNoEvent() {
        tenant("ACTIVE"); when(dao.updateStatus(any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.status("id", 2L, "CLOSED"));
        verifyNoInteractions(events);
    }
    @Test void closedDeletionIsLogicalAndAudited() {
        tenant("CLOSED"); when(dao.logicalDelete(any())).thenReturn(1);
        service.delete("id", 2L);
        ArgumentCaptor<TenantDO> saved = ArgumentCaptor.forClass(TenantDO.class);
        verify(dao).logicalDelete(saved.capture());
        assertEquals("admin-id", saved.getValue().getDeleter());
        assertEquals("admin-account", saved.getValue().getModifierCode());
        assertNotNull(saved.getValue().getDeleteTime());
        verify(events).publishEvent(any(TenantUnavailableEvent.class));
    }
    @Test void activeTenantMustBeClosedBeforeDeletion() {
        tenant("ACTIVE");
        assertThrows(BusinessException.class, () -> service.delete("id", 2L));
        verify(dao, never()).logicalDelete(any());
    }
    @Test void detailUpdatePreservesTenantIdentityStatusAndActivation() {
        TenantVO tenant = tenant("ACTIVE"); tenant.setActivatedTime("original");
        when(dao.updateDetails(any())).thenReturn(1);
        TenantDTO input = new TenantDTO(); input.setId("id"); input.setVersion(2L);
        input.setTenantCode("NEW"); input.setTenantName("New name"); input.setTenantType("ENTERPRISE");
        service.update(input);
        assertEquals("other", tenant.getTenantId()); assertEquals("ACTIVE", tenant.getStatus());
        assertEquals("original", tenant.getActivatedTime()); assertEquals("admin-account", tenant.getModifierCode());
    }
    @Test void staleClientVersionRejectedBeforeWrite() {
        tenant("PENDING");
        assertThrows(BusinessException.class, () -> service.delete("id", 1L));
        verify(dao, never()).logicalDelete(any());
    }
}
