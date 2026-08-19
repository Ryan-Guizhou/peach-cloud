package com.peach.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.LoginInfo;
import com.peach.auth.common.RsaPasswordUtil;
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
import com.peach.auth.vo.AuthFunctionVO;
import com.peach.auth.vo.AuthResourceVO;
import com.peach.auth.vo.MenuVO;
import com.peach.auth.vo.LoginInitVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.RouterVO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.auth.vo.UserVO;
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

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;

/**
 * 用户业务服务实现。
 * <p>负责用户基础信息、登录态、权限结果以及机构信息的组装，不直接承载组织树维护逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Slf4j
@Indexed
@Service
public class UserServiceImpl implements IUserService {

    private static final String INIT_APP_ID = "f73b300578a5436d82ec7fca2c07c284";

    @Resource
    private UserDao userDao;

    @Resource
    private UserOrgDao userOrgDao;

    @Resource
    private MenuDao menuDao;

    @Resource
    private RouterDao routerDao;

    @Resource
    private AuthFunctionDao authFunctionDao;

    @Resource
    private AuthResourceDao authResourceDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IRoleService iRoleService;

    @Override
    public PageInfo<UserVO> pageList(UserQO userQO) {
        return PageHelper.startPage(userQO.getPageNum(), userQO.getPageSize())
                .doSelectPageInfo(() -> userDao.selectByQO(userQO));
    }

    @Override
    public List<UserVO> list(UserQO userQO) {
        if (userQO != null && !CollectionUtils.isEmpty(userQO.getUserIdList())) {
            return userDao.selectByIds(userQO.getUserIdList());
        }
        return userDao.select(buildUserDO(userQO));
    }

    @Override
    public Response login(LoginDTO loginDTO) {
        String password = loginDTO.getPassword();
        String username = loginDTO.getUsername();
        String decryptPassword;
        try {
            decryptPassword = RsaPasswordUtil.decrypt(password);
        } catch (Exception e) {
            log.error("解密密码失败:{}", e.getMessage(), e);
            return Response.fail("密码解密失败");
        }
        if (StringUtil.isBlank(decryptPassword)) {
            return Response.fail("密码解密失败");
        }

        String base64Password = Base64.getEncoder().encodeToString(
                decryptPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        UserVO userVO = userDao.login(username, base64Password);
        if (ObjectUtil.isNull(userVO)) {
            log.warn("用户名或密码校验失败，username={}", username);
            return Response.fail("用户名或密码错误");
        }

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
        log.info("用户登录成功，userId={}, tenantId={}, orgId={}", userId,
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
        loginInfo.setIsDefaultPwd(userVO.getIsModify() == null ? null : (userVO.getIsModify() == 0 ? 1 : 0));
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
        loginInfo.setIsDefaultPwd(userVO.getIsModify() == null ? null : (userVO.getIsModify() == 0 ? 1 : 0));
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
        LoginInitVO initVO = new LoginInitVO();
        initVO.setSystemName("Peach Cloud DataOS");
        initVO.setSystemDescription("面向租户与机构的数据治理、权限和业务协同平台");
        initVO.setAppId(INIT_APP_ID);
        initVO.setFiscal(LocalDate.now().getYear());
        initVO.setPublicKey(RsaPasswordUtil.getPublicKeyBase64());
        initVO.setEncryptionAlgorithm("RSAES-PKCS1-v1_5");
        return initVO;
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

    private List<AuthResourceVO> selectResources(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Map<String, AuthResourceVO> resourceMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(roleList)) {
            return Collections.emptyList();
        }
        for (RoleVO role : roleList) {
            if (role == null || StringUtil.isBlank(role.getRoleCode())) {
                continue;
            }
            AuthResourceDO query = new AuthResourceDO();
            query.setTenantId(tenantId);
            query.setOrgId(orgId);
            query.setPartyCode(role.getRoleCode());
            query.setFiscal(fiscal);
            query.setIsDelete(PubCommonConst.LOGIC_FLASE);
            List<AuthResourceVO> resources = authResourceDao.select(query);
            if (CollectionUtils.isEmpty(resources)) {
                continue;
            }
            for (AuthResourceVO resource : resources) {
                if (resource == null || StringUtil.isBlank(resource.getResourceCode())) {
                    continue;
                }
                String key = resource.getOpType() + ":" + resource.getResourceCode();
                resourceMap.put(key, resource);
            }
        }
        return new ArrayList<>(resourceMap.values());
    }

    private List<String> buildPermissionList(List<AuthResourceVO> resourceList) {
        if (CollectionUtils.isEmpty(resourceList)) {
            return Collections.emptyList();
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
        Set<String> funcCodeSet = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(roleList)) {
            for (RoleVO roleVO : roleList) {
                if (roleVO == null || StringUtil.isBlank(roleVO.getRoleCode())) {
                    continue;
                }
                AuthFunctionDO authFunctionVOQO = new AuthFunctionDO();
                authFunctionVOQO.setTenantId(tenantId);
                authFunctionVOQO.setOrgId(orgId);
                authFunctionVOQO.setFiscal(fiscal);
                authFunctionVOQO.setPartyCode(roleVO.getRoleCode());
                authFunctionVOQO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                List<AuthFunctionVO> authFunctionVOList = authFunctionDao.select(authFunctionVOQO);
                if (CollectionUtils.isEmpty(authFunctionVOList)) {
                    continue;
                }
                for (AuthFunctionVO authFunctionVO : authFunctionVOList) {
                    if (authFunctionVO != null && StringUtil.isNotBlank(authFunctionVO.getFuncCode())) {
                        funcCodeSet.add(authFunctionVO.getFuncCode());
                    }
                }
            }
        }

        Map<String, MenuVO> menuMap = new LinkedHashMap<>();
        if (!funcCodeSet.isEmpty()) {
            for (String funcCode : funcCodeSet) {
                MenuDO menuDO = new MenuDO();
                menuDO.setAppId(INIT_APP_ID);
                menuDO.setTenantId(tenantId);
                menuDO.setFuncCode(funcCode);
                menuDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                menuDO.setIsShow(1);
                menuDO.setIsDisable(0);
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

        return Collections.emptyList();
    }

    private List<RouterVO> selectRouters(List<RoleVO> roleList, String tenantId, String orgId, Integer fiscal) {
        Set<String> routerCodeSet = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(roleList)) {
            for (RoleVO roleVO : roleList) {
                if (roleVO == null || StringUtil.isBlank(roleVO.getRoleCode())) {
                    continue;
                }
                AuthFunctionDO authFunctionVOQO = new AuthFunctionDO();
                authFunctionVOQO.setTenantId(tenantId);
                authFunctionVOQO.setOrgId(orgId);
                authFunctionVOQO.setFiscal(fiscal);
                authFunctionVOQO.setPartyCode(roleVO.getRoleCode());
                authFunctionVOQO.setIsDelete(PubCommonConst.LOGIC_FLASE);
                List<AuthFunctionVO> authFunctionVOList = authFunctionDao.select(authFunctionVOQO);
                if (CollectionUtils.isEmpty(authFunctionVOList)) {
                    continue;
                }
                for (AuthFunctionVO authFunctionVO : authFunctionVOList) {
                    if (authFunctionVO != null && StringUtil.isNotBlank(authFunctionVO.getFuncCode())) {
                        routerCodeSet.add(authFunctionVO.getFuncCode());
                    }
                }
            }
        }

        Map<String, RouterVO> routerMap = new LinkedHashMap<>();
        if (!routerCodeSet.isEmpty()) {
            for (String routerCode : routerCodeSet) {
                RouterDO routerDO = new RouterDO();
                routerDO.setAppId(INIT_APP_ID);
                routerDO.setTenantId(tenantId);
                routerDO.setRouterCode(routerCode);
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

        return Collections.emptyList();
    }

    private UserOrgVO resolveUserOrg(LoginDTO loginDTO, UserVO userVO, List<UserOrgVO> userOrgList) {
        if (StringUtil.isNotBlank(loginDTO.getTenantId()) || StringUtil.isNotBlank(loginDTO.getOrgId())) {
            for (UserOrgVO userOrgVO : userOrgList) {
                boolean tenantMatch = StringUtil.isBlank(loginDTO.getTenantId())
                        || loginDTO.getTenantId().equals(userOrgVO.getTenantId());
                boolean orgMatch = StringUtil.isBlank(loginDTO.getOrgId())
                        || loginDTO.getOrgId().equals(userOrgVO.getOrgId());
                if (tenantMatch && orgMatch) {
                    return userOrgVO;
                }
            }
        }

        if (StringUtil.isNotBlank(userVO.getDefaultOrgId())) {
            for (UserOrgVO userOrgVO : userOrgList) {
                boolean orgMatch = StringUtil.isBlank(userVO.getDefaultOrgId())
                        || userVO.getDefaultOrgId().equals(userOrgVO.getOrgId());
                if (orgMatch) {
                    return userOrgVO;
                }
            }
        }

        for (UserOrgVO userOrgVO : userOrgList) {
            if (userOrgVO.getIsDefault() != null && userOrgVO.getIsDefault() == 1) {
                return userOrgVO;
            }
        }
        return userOrgList.get(0);
    }

    @Override
    public Response register(RegisterDTO registerDTO) {
        return Response.success();
    }

    @Override
    public UserVO selectUserById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new UserVO();
        }
        return userDao.selectById(id);
    }

    @Override
    public void add(UserDTO userDTO) {
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userDTO, userDO);
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
        userDO.setMobilePhone(userQO.getPhone());
        userDO.setDefaultOrgId(userQO.getDefaultOrgId());
        return userDO;
    }
}
