package com.peach.auth.service.impl;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.peach.auth.dao.TenantDao;
import com.peach.auth.dto.TenantDTO;
import com.peach.auth.entity.TenantDO;
import com.peach.auth.event.TenantUnavailableEvent;
import com.peach.auth.qo.TenantQO;
import com.peach.auth.service.ITenantService;
import com.peach.auth.vo.TenantVO;
import com.peach.common.exception.BusinessException;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements ITenantService {
    private final TenantDao tenantDao;
    private final ApplicationEventPublisher events;

    @Override
    public PageInfo<TenantVO> pageList(TenantQO query) {
        return PageMethod.startPage(query.getPageNum(), query.getPageSize())
                .doSelectPageInfo(() -> tenantDao.selectByQO(query));
    }

    @Override
    public List<TenantVO> list(TenantQO query) { return tenantDao.selectByQO(query); }

    @Override
    public TenantVO selectById(String id) {
        TenantVO tenant = tenantDao.selectById(id);
        if (tenant == null) throw new BusinessException("租户不存在或已删除");
        return tenant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(TenantDTO input) {
        uniqueCode(input.getTenantCode(), null);
        TenantDO tenant = new TenantDO();
        details(input, tenant);
        tenant.setId(uuid());
        tenant.setTenantId(uuid());
        tenant.setStatus("PENDING");
        tenant.setDeleted(0);
        tenant.setVersion(0L);
        UserContext actor = actor();
        tenant.setCreatorId(actor.getUserId());
        tenant.setCreatorCode(actor.getUserCode());
        tenant.setCreateTime(now());
        try { tenantDao.insert(tenant); }
        catch (DuplicateKeyException ex) { throw new BusinessException("租户编码已存在"); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantDTO input) {
        TenantVO tenant = current(input.getId(), input.getVersion());
        uniqueCode(input.getTenantCode(), tenant.getId());
        details(input, tenant);
        audit(tenant);
        try { changed(tenantDao.updateDetails(tenant)); }
        catch (DuplicateKeyException ex) { throw new BusinessException("租户编码已存在"); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void status(String id, Long version, String status) {
        TenantVO tenant = current(id, version);
        String from = tenant.getStatus();
        boolean allowed = switch (from) {
            case "PENDING" -> Set.of("ACTIVE", "CLOSED").contains(status);
            case "ACTIVE" -> Set.of("SUSPENDED", "CLOSED").contains(status);
            case "SUSPENDED" -> Set.of("ACTIVE", "CLOSED").contains(status);
            default -> false;
        };
        if (!allowed) throw new BusinessException("不允许的租户状态变更");
        boolean unavailable = "SUSPENDED".equals(status) || "CLOSED".equals(status);
        if (unavailable) protectCurrent(tenant);
        tenant.setStatus(status);
        if ("ACTIVE".equals(status) && tenant.getActivatedTime() == null) tenant.setActivatedTime(now());
        if ("CLOSED".equals(status)) tenant.setClosedAt(now());
        audit(tenant);
        changed(tenantDao.updateStatus(tenant));
        if (unavailable) events.publishEvent(new TenantUnavailableEvent(tenant.getTenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, Long version) {
        TenantVO tenant = current(id, version);
        protectCurrent(tenant);
        if (!Set.of("PENDING", "CLOSED").contains(tenant.getStatus()))
            throw new BusinessException("仅待启用或已关闭租户允许删除");
        audit(tenant);
        tenant.setDeleter(actor().getUserId());
        tenant.setDeleteTime(now());
        changed(tenantDao.logicalDelete(tenant));
        events.publishEvent(new TenantUnavailableEvent(tenant.getTenantId()));
    }

    private TenantVO current(String id, Long version) {
        if (id == null || id.isBlank() || version == null || version < 0)
            throw new BusinessException("租户ID和版本号不能为空");
        TenantVO tenant = selectById(id);
        if (!Objects.equals(version, tenant.getVersion())) throw new BusinessException("数据已变更，请刷新后重试");
        return tenant;
    }

    private void protectCurrent(TenantDO tenant) {
        UserContext user = actor();
        if (Objects.equals(tenant.getTenantId(), user.getTenantId()))
            throw new BusinessException("不能停用、关闭或删除当前登录租户");
    }

    private void uniqueCode(String code, String id) {
        if (tenantDao.countByCode(code, id) > 0) throw new BusinessException("租户编码已存在");
    }

    private static void changed(int count) {
        if (count != 1) throw new BusinessException("数据已变更，请刷新后重试");
    }

    private static UserContext actor() {
        UserContext user = SecurityContextHolder.get();
        if (user == null || user.getUserId() == null || user.getUserCode() == null)
            throw new BusinessException("登录上下文缺失");
        return user;
    }

    private static void audit(TenantDO tenant) {
        UserContext user = actor();
        tenant.setModifierId(user.getUserId());
        tenant.setModifierCode(user.getUserCode());
        tenant.setModifyTime(now());
    }

    private static void details(TenantDTO input, TenantDO tenant) {
        tenant.setTenantCode(input.getTenantCode());
        tenant.setTenantName(input.getTenantName());
        tenant.setTenantType(input.getTenantType());
        tenant.setDescription(input.getDescription());
        tenant.setContactName(input.getContactName());
        tenant.setContactPhone(input.getContactPhone());
        tenant.setContactEmail(input.getContactEmail());
    }
    private static String now() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    private static String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
}
