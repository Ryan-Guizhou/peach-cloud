package com.peach.auth.service.impl;

import com.peach.auth.dao.*;
import com.peach.auth.dto.RoleWorkspaceDTO;
import com.peach.auth.entity.*;
import com.peach.auth.qo.*;
import com.peach.auth.vo.*;
import com.peach.auth.service.support.*;
import com.peach.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色工作区：限定当前上下文，原子保存功能和资源授权，校准只返回待清理差异。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleWorkspaceService {
    private final RoleDao roleDao;
    private final RoleWorkspaceDao dao;
    private final FunctionDao functionDao;
    private final ResourceDao resourceDao;
    private final LoginPermissionCacheRefresher refresher;
    private final AuthLogSupport audit;

    /** 完整快照，不将界面筛选后的子集作为全量授权事实。 */
    public RoleWorkspaceVO get(String id) { permission("role:view"); return snapshot(role(id)); }
    public RoleMembersVO members(String id) {
        permission("authorization:update"); RoleVO role = role(id);
        RoleMembersVO result = new RoleMembersVO();
        var users = dao.users(role); users.forEach(UserSensitiveFieldSupport::decryptUserFields);
        result.setUsers(users); result.setUserCodes(dao.memberCodes(role)); result.setRevision(memberRevision(role, result.getUserCodes()));
        return result;
    }
    /** 两类授权在同一事务更新；修订号不匹配时拒绝覆盖，缓存仅在提交后刷新。 */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void saveGrants(RoleWorkspaceDTO input) {
        permission("authorization:update"); RoleVO role = locked(input.getRoleId());
        Set<String> functions = required(input.getFuncCodes()); Set<String> resources = required(input.getResourceKeys());
        for (String code : functions.stream().sorted().toList()) {
            FunctionDO definition = new FunctionDO(); definition.setTenantId(role.getTenantId()); definition.setAppId(AdminScope.APP_ID); definition.setFuncCode(code);
            functionDao.lockByCode(definition);
        }
        RoleWorkspaceVO current = snapshot(role); revision(input.getRevision(), current.getRevision());
        Map<String, FunctionVO> available = current.getFunctions().stream().filter(f -> !Integer.valueOf(1).equals(f.getIsDisable())).collect(Collectors.toMap(FunctionVO::getFuncCode, Function.identity(), (a,b) -> a));
        Set<String> original = new HashSet<>(current.getFuncCodes());
        for (String code : functions) if (!available.containsKey(code) && !original.contains(code)) throw new BusinessException("功能不存在、已禁用或不属于当前应用");
        Map<String, ResourceVO> definitions = current.getResources().stream().collect(Collectors.toMap(r -> key(r.getFuncCode(),r.getResourceType(),r.getResourceCode()), Function.identity(), (a,b) -> a));
        Map<String, AuthResourceVO> existing = current.getResourceGrants().stream().collect(Collectors.toMap(r -> key(r.getFuncCode(),r.getOpType(),r.getResourceCode()), Function.identity(), (a,b) -> a));
        List<AuthResourceDO> granted = new ArrayList<>();
        for (String resourceKey : resources) {
            ResourceVO resource = definitions.get(resourceKey); AuthResourceVO previous = existing.get(resourceKey);
            String funcCode = resource == null ? previous == null ? null : previous.getFuncCode() : resource.getFuncCode();
            if (funcCode == null || !functions.contains(funcCode)) throw new BusinessException("资源必须属于当前选中的功能");
            if (resource == null && previous == null) throw new BusinessException("资源不存在或不属于当前应用");
            if (resource != null && !available.containsKey(funcCode) && previous == null) throw new BusinessException("不可为禁用功能新增资源授权");
            AuthResourceDO row = new AuthResourceDO();
            row.setResourceId(id()); row.setTenantId(role.getTenantId()); row.setOrgId(role.getOrgId()); row.setPartyCode(role.getRoleCode()); row.setFiscal(role.getFiscal()); row.setAppId(AdminScope.APP_ID);
            row.setFuncCode(funcCode); row.setOpType(resource == null ? previous.getOpType() : resource.getResourceType());
            row.setResourceCode(resource == null ? previous.getResourceCode() : resource.getResourceCode()); row.setResourceName(resource == null ? previous.getResourceName() : resource.getResourceName());
            row.fillCreateTime(AdminScope.actor().getUserId()); granted.add(row);
        }
        dao.deleteResources(role); dao.deleteFunctions(role);
        for (String code : functions) {
            AuthFunctionDO row = new AuthFunctionDO(); row.setId(id()); row.setTenantId(role.getTenantId()); row.setOrgId(role.getOrgId()); row.setPartyCode(role.getRoleCode()); row.setFiscal(role.getFiscal()); row.setAppId(AdminScope.APP_ID); row.setFuncCode(code); row.fillCreateTime(AdminScope.actor().getUserId()); dao.insertFunction(row);
        }
        for (AuthResourceDO row : granted) dao.insertResource(row);
        if (role.getSkipUrl() != null && !role.getSkipUrl().isBlank() && dao.menus(role).stream().noneMatch(menu -> role.getSkipUrl().equals(menu.getMenuUrl()))) dao.updateDefault(role, "");
        audit.recordRoleGrant(role.getTenantId(),role.getOrgId(),role.getRoleCode(),"角色工作区授权，功能数量："+functions.size()+"，资源数量："+resources.size());
        afterCommit(() -> refresher.refreshUsersByRole(role.getTenantId(),role.getOrgId(),role.getRoleCode(),role.getFiscal()));
    }
    /** 只生成差异，不新增授权，不自动写库。 */
    public RoleCalibrationVO calibrate(RoleWorkspaceDTO input) {
        permission("authorization:update"); RoleWorkspaceVO current = snapshot(role(input.getRoleId())); revision(input.getRevision(), current.getRevision());
        Set<String> chosen = required(input.getFuncCodes()); Set<String> resources = required(input.getResourceKeys());
        Set<String> active = current.getFunctions().stream().filter(f -> !Integer.valueOf(1).equals(f.getIsDisable())).map(FunctionVO::getFuncCode).collect(Collectors.toSet());
        Set<String> allowed = current.getResources().stream().filter(r -> active.contains(r.getFuncCode()) && chosen.contains(r.getFuncCode())).map(r -> key(r.getFuncCode(),r.getResourceType(),r.getResourceCode())).collect(Collectors.toSet());
        RoleCalibrationVO result = new RoleCalibrationVO();
        result.setRemovedFuncCodes(chosen.stream().filter(code -> !active.contains(code)).sorted().toList());
        result.setRemovedResourceKeys(resources.stream().filter(code -> !allowed.contains(code)).sorted().toList());
        return result;
    }
    /** 仅替换本角色的成员，保留用户的其他角色；修订号防止旧成员列表覆盖新变更。 */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void saveMembers(RoleWorkspaceDTO input) {
        permission("authorization:update"); RoleVO role = locked(input.getRoleId());
        List<String> old = dao.memberCodes(role); revision(input.getRevision(), memberRevision(role, old));
        Set<String> desired = required(input.getUserCodes());
        Set<String> allowed = dao.users(role).stream().map(UserVO::getUserCode).collect(Collectors.toSet());
        allowed.addAll(old);
        if (!allowed.containsAll(desired)) throw new BusinessException("成员必须属于当前租户机构");
        dao.deleteMembers(role);
        for (String code : desired) {
            AuthPartyDO row = new AuthPartyDO(); row.setId(id()); row.setTenantId(role.getTenantId()); row.setOrgId(role.getOrgId()); row.setRoleCode(role.getRoleCode()); row.setRoleType(role.getRoleType() == null ? "CUSTOM" : role.getRoleType()); row.setFiscal(role.getFiscal()); row.setPartyCode(code); row.fillCreateTime(AdminScope.actor().getUserId()); dao.insertMember(row);
        }
        Set<String> affected = new HashSet<>(old); affected.addAll(desired);
        audit.recordRoleGrant(role.getTenantId(),role.getOrgId(),role.getRoleCode(),"角色成员维护，用户数量："+desired.size());
        afterCommit(() -> { for (String code : affected) refresher.refreshUserByCode(role.getTenantId(),role.getOrgId(),code,role.getFiscal()); });
    }
    /** 默认菜单必须是该角色已保存授权中的可用菜单；空值恢复工作台。 */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void saveDefault(RoleWorkspaceDTO input) {
        permission("role:update"); RoleVO role = locked(input.getRoleId()); RoleWorkspaceVO current = snapshot(role); revision(input.getRevision(), current.getRevision());
        String url = input.getSkipUrl() == null ? "" : input.getSkipUrl();
        if (!url.isBlank() && (!url.startsWith("/") || url.startsWith("//") || current.getMenus().stream().noneMatch(m -> url.equals(m.getMenuUrl())))) throw new BusinessException("默认菜单必须来自当前角色已授权的菜单");
        dao.updateDefault(role,url);
    }
    private RoleWorkspaceVO snapshot(RoleVO role) {
        var result = new RoleWorkspaceVO(); result.setRole(role);
        FunctionQO functions = new FunctionQO(); functions.setTenantId(role.getTenantId()); functions.setAppId(AdminScope.APP_ID);
        ResourceQO resources = new ResourceQO(); resources.setTenantId(role.getTenantId()); resources.setAppId(AdminScope.APP_ID);
        result.setFunctions(functionDao.selectByQO(functions)); result.setResources(resourceDao.selectByQO(resources));
        result.setFuncCodes(dao.functions(role)); result.setResourceGrants(dao.resources(role)); result.setMenus(dao.menus(role));
        result.setRevision(digest(part(result.getFuncCodes().stream().sorted().map(RoleWorkspaceService::part).collect(Collectors.joining()))+part(result.getResourceGrants().stream().map(r -> key(r.getFuncCode(),r.getOpType(),r.getResourceCode())).sorted().map(RoleWorkspaceService::part).collect(Collectors.joining()))+part(role.getSkipUrl())));
        return result;
    }
    private RoleVO role(String id) {
        RoleVO row = roleDao.selectById(id); var actor = AdminScope.actor();
        if (row == null || Integer.valueOf(1).equals(row.getIsDelete()) || !Objects.equals(row.getTenantId(),actor.getTenantId()) || !Objects.equals(row.getOrgId(),actor.getOrgId()) || !Objects.equals(row.getFiscal(),AdminScope.fiscal())) throw new BusinessException("角色不存在或不可访问");
        return row;
    }
    private RoleVO locked(String id) {
        RoleVO row = dao.lockRole(id,AdminScope.actor()); if (row == null) throw new BusinessException("角色不存在或不可访问"); return row;
    }
    private void permission(String value) { if (dao.hasPermission(AdminScope.actor(),value) == 0) throw new BusinessException("无角色维护权限"); }
    private static Set<String> required(List<String> values) { if (values == null) throw new BusinessException("缺少完整的授权或成员列表"); return new LinkedHashSet<>(values); }
    private static void revision(String supplied,String actual) { if (!Objects.equals(supplied,actual)) throw new BusinessException("角色授权已变化，请刷新后重试"); }
    private static String memberRevision(RoleVO role,List<String> users) { return digest(part(role.getRoleId())+users.stream().sorted().map(RoleWorkspaceService::part).collect(Collectors.joining())); }
    /** 长度前缀身份编码，与前端一致，避免分隔符出现在资源编码时发生碰撞。 */
    public static String key(String function,String type,String code) { return part(function)+part(type)+part(code); }
    private static String part(String value) { String text = Objects.toString(value,""); return text.length()+":"+text; }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
    }
    private static String id() { return UUID.randomUUID().toString().replace("-", ""); }
    private static void afterCommit(Runnable refresh) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { try { refresh.run(); } catch (RuntimeException ex) { log.error("Role permission cache refresh failed after commit, errorType={}", ex.getClass().getSimpleName()); } }
        });
        else refresh.run();
    }
}
