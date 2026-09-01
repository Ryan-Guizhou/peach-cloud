package com.peach.auth.service.impl;

import java.nio.charset.StandardCharsets;
import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageInfo;
import com.peach.auth.LoginInfo;
import com.peach.auth.common.RsaPasswordUtil;
import com.peach.auth.common.SensitiveFieldCipher;
import com.peach.auth.dao.AuthFunctionDao;
import com.peach.auth.dao.AuthResourceDao;
import com.peach.auth.dao.MenuDao;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.SwitchContextDTO;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.AuthFunctionDO;
import com.peach.auth.entity.AuthResourceDO;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.IUserService;
import com.peach.auth.service.LoginLockService;
import com.peach.auth.service.support.LoginInitConfigSupport;
import com.peach.auth.service.support.UserSensitiveFieldSupport;
import com.peach.auth.vo.AuthFunctionVO;
import com.peach.auth.vo.AuthResourceVO;
import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.LoginInitVO;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.auth.vo.UserVO;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.satoken.constant.SatokenConstant;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户服务实现类。
 * <p>负责用户基础信息、登录态、权限结果以及机构信息的组装，不直接承载组织树维护逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private static final String INIT_APP_ID = "f73b300578a5436d82ec7fca2c07c284";

    private final UserDao userDao;

    private final UserOrgDao userOrgDao;

    private final MenuDao menuDao;

    private final RouterDao routerDao;

    private final AuthFunctionDao authFunctionDao;

    private final AuthResourceDao authResourceDao;

    private final StringRedisTemplate stringRedisTemplate;

    private final IRoleService iRoleService;

    private final CaptchaService captchaService;

    private final LoginLockService loginLockService;

    private final LoginInitConfigSupport loginInitConfigSupport;


    @Override
    public PageInfo<UserVO> pageList(UserQO userQO) {
        PageInfo<UserVO> pageInfo = PageMethod.startPage(userQO.getPageNum(), userQO.getPageSize())
                .doSelectPageInfo(() -> userDao.selectByQO(userQO));
        if (pageInfo.getList() != null) {
            pageInfo.getList().forEach(UserSensitiveFieldSupport::decryptUserFields);
        }
        return pageInfo;
    }

    @Override
    public List<UserVO> list(UserQO userQO) {
        List<UserVO> users;
        if (userQO != null && !CollectionUtils.isEmpty(userQO.getUserIdList())) {
            users = userDao.selectByIds(userQO.getUserIdList());
        } else {
            users = userDao.select(buildUserDO(userQO));
        }
        if (users != null) {
            users.forEach(UserSensitiveFieldSupport::decryptUserFields);
        }
        return users;
    }

    @Override
    public Response login(LoginDTO loginDTO) {
        Response captchaResponse = verifyLoginCaptcha(loginDTO.getCaptchaVerification(), loginDTO.getClientUid());
        if (!captchaResponse.isSuccess()) {
            return captchaResponse;
        }

        String username = loginDTO.getUsername().trim();
        LoginLockStatusVO lockStatus = loginLockService.checkLock(username);
        if (lockStatus.isLocked()) {
            return Response.fail(formatLockMessage(lockStatus));
        }

        String password = loginDTO.getPassword();
        String decryptPassword;
        try {
            decryptPassword = RsaPasswordUtil.decrypt(password);
        } catch (Exception e) {
            log.error("Failed to decrypt password: {}", e.getMessage(), e);
            return Response.fail("密码解密失败");
        }
        if (StringUtil.isBlank(decryptPassword)) {
            return Response.fail("密码解密失败");
        }

        String base64Password = Base64.getEncoder().encodeToString(
                decryptPassword.getBytes(StandardCharsets.UTF_8));
        UserVO userVO = userDao.login(username, base64Password);
        if (ObjectUtil.isNull(userVO)) {
            log.warn("Username or password validation failed, username={}", username);
            LoginLockStatusVO failureStatus = loginLockService.recordFailure(username);
            if (failureStatus.isLocked()) {
                return Response.fail(formatLockMessage(failureStatus));
            }
            return Response.fail("用户名或密码错误");
        }
        loginLockService.clearOnSuccess(username);
        UserSensitiveFieldSupport.decryptUserFields(userVO);

        String userId = userVO.getUserId();
        List<UserOrgVO> userOrgList = userOrgDao.selectByUserId(userId);
        if (CollectionUtils.isEmpty(userOrgList)) {
            return Response.fail("当前用户未绑定租户或机构");
        }

        UserOrgVO currentUserOrg = resolveUserOrg(loginDTO, userVO, userOrgList);
        if (currentUserOrg == null) {
            return Response.fail("未找到匹配的租户或机构");
        }

        StpUtil.login(userId);
        String token = StpUtil.getTokenValue();
        log.info("User login succeeded, userId={}, tenantId={}, orgId={}", userId,
                currentUserOrg.getTenantId(), currentUserOrg.getOrgId());

        RoleQO roleQO = new RoleQO();
        roleQO.setFiscal(loginDTO.getFiscal());
        roleQO.setUserCode(userVO.getUserCode());
        roleQO.setTenantId(currentUserOrg.getTenantId());
        roleQO.setOrgId(currentUserOrg.getOrgId());
        List<RoleVO> roleList = iRoleService.selectByUserCode(roleQO);
        List<AuthResourceVO> resourceList = selectResources(roleList, currentUserOrg.getTenantId(),
                currentUserOrg.getOrgId(), loginDTO.getFiscal());
        refreshLoginPermissionCache(userVO, currentUserOrg, loginDTO.getFiscal(), resourceList);
        List<MenuVO> menuList = selectMenus(roleList, currentUserOrg.getTenantId(), currentUserOrg.getOrgId(), loginDTO.getFiscal());
        List<RouterVO> routerList = selectRouters(roleList, currentUserOrg.getTenantId(), currentUserOrg.getOrgId(), loginDTO.getFiscal());

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setUserId(userVO.getUserId());
        loginInfo.setUserName(userVO.getUserName());
        loginInfo.setFiscal(String.valueOf(loginDTO.getFiscal()));
        loginInfo.setTenantId(currentUserOrg.getTenantId());
        loginInfo.setTenantCode(currentUserOrg.getTenantCode());
        loginInfo.setTenantName(currentUserOrg.getTenantName());
        loginInfo.setOrgId(currentUserOrg.getOrgId());
        loginInfo.setOrgCode(currentUserOrg.getOrgCode());
        loginInfo.setOrgName(currentUserOrg.getOrgName());
        loginInfo.setIsDefaultPwd(resolveIsDefaultPwd(userVO.getIsModify()));
        loginInfo.setUserOrgList(userOrgList);
        loginInfo.setRoleList(roleList);
        loginInfo.setMenuList(menuList);
        loginInfo.setRouterList(routerList);
        loginInfo.setResourceList(resourceList);
        loginInfo.setPermissionList(buildPermissionList(resourceList));
        loginInfo.setToken(token);
        return Response.success(loginInfo);
    }

    @Override
    public Response switchContext(SwitchContextDTO switchContextDTO) {
        String userId = StpUtil.getLoginIdAsString();
        UserVO userVO = userDao.selectById(userId);
        if (ObjectUtil.isNull(userVO)) {
            return Response.fail("当前用户不存在");
        }
        UserSensitiveFieldSupport.decryptUserFields(userVO);
        List<UserOrgVO> userOrgList = userOrgDao.selectByUserIdAndTenantId(userId, switchContextDTO.getTenantId());
        if (CollectionUtils.isEmpty(userOrgList)) {
            return Response.fail("当前用户未绑定目标租户");
        }
        UserOrgVO currentUserOrg = null;
        for (UserOrgVO userOrgVO : userOrgList) {
            if (userOrgVO != null && switchContextDTO.getOrgId().equals(userOrgVO.getOrgId())) {
                currentUserOrg = userOrgVO;
                break;
            }
        }
        if (currentUserOrg == null) {
            return Response.fail("当前用户未绑定目标机构");
        }

        RoleQO roleQO = new RoleQO();
        roleQO.setFiscal(switchContextDTO.getFiscal());
        roleQO.setUserCode(userVO.getUserCode());
        roleQO.setTenantId(currentUserOrg.getTenantId());
        roleQO.setOrgId(currentUserOrg.getOrgId());
        List<RoleVO> roleList = iRoleService.selectByUserCode(roleQO);
        List<AuthResourceVO> resourceList = selectResources(roleList, currentUserOrg.getTenantId(),
                currentUserOrg.getOrgId(), switchContextDTO.getFiscal());
        refreshLoginPermissionCache(userVO, currentUserOrg, switchContextDTO.getFiscal(), resourceList);
        List<MenuVO> menuList = selectMenus(roleList, currentUserOrg.getTenantId(),
                currentUserOrg.getOrgId(), switchContextDTO.getFiscal());
        List<RouterVO> routerList = selectRouters(roleList, currentUserOrg.getTenantId(),
                currentUserOrg.getOrgId(), switchContextDTO.getFiscal());

        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setUserId(userVO.getUserId());
        loginInfo.setUserName(userVO.getUserName());
        loginInfo.setFiscal(String.valueOf(switchContextDTO.getFiscal()));
        loginInfo.setTenantId(currentUserOrg.getTenantId());
        loginInfo.setTenantCode(currentUserOrg.getTenantCode());
        loginInfo.setTenantName(currentUserOrg.getTenantName());
        loginInfo.setOrgId(currentUserOrg.getOrgId());
        loginInfo.setOrgCode(currentUserOrg.getOrgCode());
        loginInfo.setOrgName(currentUserOrg.getOrgName());
        loginInfo.setIsDefaultPwd(resolveIsDefaultPwd(userVO.getIsModify()));
        loginInfo.setUserOrgList(userOrgDao.selectByUserId(userId));
        loginInfo.setRoleList(roleList);
        loginInfo.setMenuList(menuList);
        loginInfo.setRouterList(routerList);
        loginInfo.setResourceList(resourceList);
        loginInfo.setPermissionList(buildPermissionList(resourceList));
        loginInfo.setToken(StpUtil.getTokenValue());
        return Response.success(loginInfo);
    }

    @Override
    public LoginInitVO initLogin() {
        return loginInitConfigSupport.buildLoginInit(RsaPasswordUtil.getPublicKeyBase64());
    }

    private Response verifyLoginCaptcha(String captchaVerification, String clientUid) {
        if (!loginInitConfigSupport.isCaptchaRequired()) {
            return Response.success();
        }
        if (StringUtil.isBlank(captchaVerification)) {
            return Response.fail("请先完成滑块验证");
        }
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(loginInitConfigSupport.resolveCaptchaType());
        captchaVO.setCaptchaVerification(captchaVerification.trim());
        if (StringUtil.isNotBlank(clientUid)) {
            captchaVO.setClientUid(clientUid.trim());
        }
        Response response = captchaService.verification(captchaVO);
        if (response == null || !response.isSuccess()) {
            return Response.fail("滑块验证未通过或已失效，请重新验证");
        }
        return response;
    }

    private String formatLockMessage(LoginLockStatusVO lockStatus) {
        if (lockStatus.isPermanent()) {
            return "账号因密码错误次数过多已被永久锁定，请联系管理员";
        }
        long minutes = Math.max(1L, (lockStatus.getRemainingLockSeconds() + 59) / 60);
        return "账号已锁定，请 " + minutes + " 分钟后再试";
    }

    /**
     * 将当前登录上下文和 BUTTON/API 资源授权写入网关共享读模型。
     * <p>数据库授权关系仍是事实源；缓存仅用于请求期快速判断。</p>
     */
    private void refreshLoginPermissionCache(UserVO userVO, UserOrgVO currentUserOrg,
                                             Integer fiscal, List<AuthResourceVO> resourceList) {
        String userId = userVO.getUserId();
        String profileKey = SatokenConstant.USER_PROFILE_CACHE_PREFIX + userId;
        String apiKey = "peach:security:user:api-resources:" + userId;
        String buttonKey = "peach:security:user:button-resources:" + userId;
        stringRedisTemplate.delete(profileKey);
        stringRedisTemplate.delete(apiKey);
        stringRedisTemplate.delete(buttonKey);

        Map<String, String> profile = new LinkedHashMap<>();
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_ID, userId);
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_CODE, userVO.getUserCode());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_USER_NAME, userVO.getUserName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_TENANT_ID, currentUserOrg.getTenantId());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_TENANT_NAME, currentUserOrg.getTenantName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_ID, currentUserOrg.getOrgId());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_CODE, currentUserOrg.getOrgCode());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_ORG_NAME, currentUserOrg.getOrgName());
        profile.put(SatokenConstant.USER_PROFILE_FIELD_FISCAL, fiscal == null ? "" : String.valueOf(fiscal));
        profile.put(SatokenConstant.USER_PROFILE_FIELD_CONTEXT_VERSION, "1");
        stringRedisTemplate.opsForHash().putAll(profileKey, profile);

        Set<String> apiResources = new LinkedHashSet<>();
        Set<String> buttonResources = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(resourceList)) {
            for (AuthResourceVO resource : resourceList) {
                if (resource == null || StringUtil.isBlank(resource.getResourceCode())) {
                    continue;
                }
                if ("API".equalsIgnoreCase(resource.getOpType())) {
                    apiResources.add(resource.getResourceCode());
                } else if ("BUTTON".equalsIgnoreCase(resource.getOpType())) {
                    buttonResources.add(resource.getResourceCode());
                }
            }
        }
        if (!apiResources.isEmpty()) {
            stringRedisTemplate.opsForSet().add(apiKey, apiResources.toArray(new String[0]));
        }
        if (!buttonResources.isEmpty()) {
            stringRedisTemplate.opsForSet().add(buttonKey, buttonResources.toArray(new String[0]));
        }
    }

    private Integer resolveIsDefaultPwd(Integer isModify) {
        if (isModify == null) {
            return null;
        }
        return isModify == 0 ? 1 : 0;
    }

    private List<AuthResourceVO> selectResources(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Map<String, AuthResourceVO> resourceMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(roleList)) {
            return List.of();
        }
        for (RoleVO role : roleList) {
            mergeResourcesFromRole(resourceMap, role, tenantId, orgId, fiscal);
        }
        return new ArrayList<>(resourceMap.values());
    }

    private void mergeResourcesFromRole(Map<String, AuthResourceVO> resourceMap, RoleVO role,
                                        String tenantId, String orgId, Integer fiscal) {
        if (role == null || StringUtil.isBlank(role.getRoleCode())) {
            return;
        }
        AuthResourceDO query = new AuthResourceDO();
        query.setTenantId(tenantId);
        query.setOrgId(orgId);
        query.setPartyCode(role.getRoleCode());
        query.setFiscal(fiscal);
        query.setIsDelete(PubCommonConst.LOGIC_FLASE);
        List<AuthResourceVO> resources = authResourceDao.select(query);
        if (CollectionUtils.isEmpty(resources)) {
            return;
        }
        for (AuthResourceVO resource : resources) {
            if (resource == null || StringUtil.isBlank(resource.getResourceCode())) {
                continue;
            }
            String key = resource.getOpType() + ":" + resource.getResourceCode();
            resourceMap.put(key, resource);
        }
    }

    private List<String> buildPermissionList(List<AuthResourceVO> resourceList) {
        if (CollectionUtils.isEmpty(resourceList)) {
            return List.of();
        }
        Set<String> permissionSet = new LinkedHashSet<>();
        for (AuthResourceVO resourceVO : resourceList) {
            if (resourceVO != null && StringUtil.isNotBlank(resourceVO.getResourceCode())) {
                permissionSet.add(resourceVO.getResourceCode());
            }
        }
        return new ArrayList<>(permissionSet);
    }

    private List<MenuVO> selectMenus(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Set<String> funcCodeSet = collectFuncCodesFromRoles(roleList, tenantId, orgId, fiscal);
        Map<String, MenuVO> menuMap = new LinkedHashMap<>();
        if (!funcCodeSet.isEmpty()) {
            for (String funcCode : funcCodeSet) {
                MenuDO menuDO = buildMenuQuery(tenantId, funcCode);
                List<MenuVO> menuVOList = menuDao.select(menuDO);
                if (CollectionUtils.isEmpty(menuVOList)) {
                    continue;
                }
                for (MenuVO menuVO : menuVOList) {
                    if (menuVO != null && StringUtil.isNotBlank(menuVO.getMenuId())) {
                        menuMap.put(menuVO.getMenuId(), menuVO);
                    }
                }
            }
            return new ArrayList<>(menuMap.values());
        }

        return List.of();
    }

    private List<RouterVO> selectRouters(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Set<String> routerCodeSet = collectFuncCodesFromRoles(roleList, tenantId, orgId, fiscal);
        Map<String, RouterVO> routerMap = new LinkedHashMap<>();
        if (!routerCodeSet.isEmpty()) {
            for (String routerCode : routerCodeSet) {
                RouterDO routerDO = buildRouterQuery(tenantId, routerCode);
                List<RouterVO> routerVOList = routerDao.select(routerDO);
                if (CollectionUtils.isEmpty(routerVOList)) {
                    continue;
                }
                for (RouterVO routerVO : routerVOList) {
                    if (routerVO != null && StringUtil.isNotBlank(routerVO.getRouterId())) {
                        routerMap.put(routerVO.getRouterId(), routerVO);
                    }
                }
            }
            return new ArrayList<>(routerMap.values());
        }

        return List.of();
    }

    private Set<String> collectFuncCodesFromRoles(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Set<String> funcCodeSet = new LinkedHashSet<>();
        if (CollectionUtils.isEmpty(roleList)) {
            return funcCodeSet;
        }
        for (RoleVO roleVO : roleList) {
            addFuncCodesFromRole(funcCodeSet, roleVO, tenantId, orgId, fiscal);
        }
        return funcCodeSet;
    }

    private void addFuncCodesFromRole(Set<String> funcCodeSet, RoleVO roleVO,
                                      String tenantId, String orgId, Integer fiscal) {
        if (roleVO == null || StringUtil.isBlank(roleVO.getRoleCode())) {
            return;
        }
        AuthFunctionDO authFunctionVOQO = new AuthFunctionDO();
        authFunctionVOQO.setTenantId(tenantId);
        authFunctionVOQO.setOrgId(orgId);
        authFunctionVOQO.setFiscal(fiscal);
        authFunctionVOQO.setPartyCode(roleVO.getRoleCode());
        authFunctionVOQO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        List<AuthFunctionVO> authFunctionVOList = authFunctionDao.select(authFunctionVOQO);
        if (CollectionUtils.isEmpty(authFunctionVOList)) {
            return;
        }
        for (AuthFunctionVO authFunctionVO : authFunctionVOList) {
            if (authFunctionVO != null && StringUtil.isNotBlank(authFunctionVO.getFuncCode())) {
                funcCodeSet.add(authFunctionVO.getFuncCode());
            }
        }
    }

    private MenuDO buildMenuQuery(String tenantId, String funcCode) {
        MenuDO menuDO = new MenuDO();
        menuDO.setAppId(INIT_APP_ID);
        menuDO.setTenantId(tenantId);
        menuDO.setFuncCode(funcCode);
        menuDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        menuDO.setIsShow(1);
        menuDO.setIsDisable(0);
        return menuDO;
    }

    private RouterDO buildRouterQuery(String tenantId, String routerCode) {
        RouterDO routerDO = new RouterDO();
        routerDO.setAppId(INIT_APP_ID);
        routerDO.setTenantId(tenantId);
        routerDO.setRouterCode(routerCode);
        return routerDO;
    }

    private UserOrgVO resolveUserOrg(LoginDTO loginDTO, UserVO userVO, List<UserOrgVO> userOrgList) {
        UserOrgVO matched = matchUserOrgByLoginSelection(loginDTO, userOrgList);
        if (matched != null) {
            return matched;
        }
        matched = matchUserOrgByDefaultOrgId(userVO, userOrgList);
        if (matched != null) {
            return matched;
        }
        matched = matchDefaultUserOrg(userOrgList);
        if (matched != null) {
            return matched;
        }
        return userOrgList.get(0);
    }

    private UserOrgVO matchUserOrgByLoginSelection(LoginDTO loginDTO, List<UserOrgVO> userOrgList) {
        if (StringUtil.isBlank(loginDTO.getTenantId()) && StringUtil.isBlank(loginDTO.getOrgId())) {
            return null;
        }
        for (UserOrgVO userOrgVO : userOrgList) {
            boolean tenantMatch = StringUtil.isBlank(loginDTO.getTenantId())
                    || loginDTO.getTenantId().equals(userOrgVO.getTenantId());
            boolean orgMatch = StringUtil.isBlank(loginDTO.getOrgId())
                    || loginDTO.getOrgId().equals(userOrgVO.getOrgId());
            if (tenantMatch && orgMatch) {
                return userOrgVO;
            }
        }
        return null;
    }

    private UserOrgVO matchUserOrgByDefaultOrgId(UserVO userVO, List<UserOrgVO> userOrgList) {
        if (StringUtil.isBlank(userVO.getDefaultOrgId())) {
            return null;
        }
        for (UserOrgVO userOrgVO : userOrgList) {
            if (userVO.getDefaultOrgId().equals(userOrgVO.getOrgId())) {
                return userOrgVO;
            }
        }
        return null;
    }

    private UserOrgVO matchDefaultUserOrg(List<UserOrgVO> userOrgList) {
        for (UserOrgVO userOrgVO : userOrgList) {
            if (userOrgVO.getIsDefault() != null && userOrgVO.getIsDefault() == 1) {
                return userOrgVO;
            }
        }
        return null;
    }

    @Override
    public Response register(RegisterDTO registerDTO) {
        return Response.success();
    }

    @Override
    public UserVO selectUserById(String id) {
        UserVO userVO = userDao.selectById(id);
        UserSensitiveFieldSupport.decryptUserFields(userVO);
        return userVO;
    }

    @Override
    public void add(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userDTO, userDO);
        UserSensitiveFieldSupport.encryptUserFields(userDO);
        userDO.fillCreateTime(null);
        if (userDO.getIsDelete() == null) {
            userDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        userDao.insert(userDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        userDao.delById(id);
    }

    @Override
    public void update(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userDTO, userDO);
        UserSensitiveFieldSupport.encryptUserFields(userDO);
        userDO.fillModifyTime(null);
        userDao.updateById(userDO);
    }

    private UserDO buildUserDO(UserQO userQO) {
        UserDO userDO = new UserDO();
        if (userQO == null) {
            return userDO;
        }
        userDO.setUserId(userQO.getUserId());
        userDO.setUserName(userQO.getUsername());
        userDO.setMobilePhone(SensitiveFieldCipher.encrypt(userQO.getPhone()));
        userDO.setDefaultOrgId(userQO.getDefaultOrgId());
        return userDO;
    }
}
