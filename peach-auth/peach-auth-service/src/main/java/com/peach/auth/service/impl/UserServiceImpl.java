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
import com.peach.auth.dao.UserDao;
import com.peach.auth.dao.UserOrgDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.SwitchContextDTO;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.service.IUserService;
import com.peach.auth.service.LoginLockService;
import com.peach.auth.service.assembler.LoginPermissionAssembler;
import com.peach.auth.service.support.LoginPermissionCacheRefresher;
import com.peach.auth.service.support.LoginInitConfigSupport;
import com.peach.auth.service.support.UserSensitiveFieldSupport;
import com.peach.auth.vo.LoginInitVO;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.RoleVO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.auth.vo.UserVO;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Base64;
import java.util.List;

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

    private final UserDao userDao;

    private final UserOrgDao userOrgDao;

    private final IRoleService iRoleService;

    private final CaptchaService captchaService;

    private final LoginLockService loginLockService;

    private final LoginInitConfigSupport loginInitConfigSupport;

    private final LoginPermissionAssembler loginPermissionAssembler;

    private final LoginPermissionCacheRefresher loginPermissionCacheRefresher;


    @Override
    public PageInfo<UserVO> pageList(UserQO userQO) {
        PageInfo<UserVO> pageInfo = PageMethod.startPage(userQO.getPageNum(), userQO.getPageSize())
                .doSelectPageInfo(() -> userDao.selectByQO(userQO));
        if (pageInfo.getList() != null) {
            pageInfo.getList().forEach(UserSensitiveFieldSupport::decryptAndMaskUserFields);
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
            users.forEach(UserSensitiveFieldSupport::decryptAndMaskUserFields);
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
        LoginPermissionSnapshotVO snapshot = loginPermissionAssembler.assemble(
                currentUserOrg, loginDTO.getFiscal(), roleList);
        refreshLoginPermissionCache(userVO, currentUserOrg, loginDTO.getFiscal(), snapshot);

        LoginInfo loginInfo = buildLoginInfo(userVO, currentUserOrg, loginDTO.getFiscal(), token, userOrgList, snapshot);
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
        LoginPermissionSnapshotVO snapshot = loginPermissionAssembler.assemble(
                currentUserOrg, switchContextDTO.getFiscal(), roleList);
        refreshLoginPermissionCache(userVO, currentUserOrg, switchContextDTO.getFiscal(), snapshot);

        log.info("User context switched, userId={}, tenantId={}, orgId={}, fiscal={}",
                userId, currentUserOrg.getTenantId(), currentUserOrg.getOrgId(), switchContextDTO.getFiscal());

        LoginInfo loginInfo = buildLoginInfo(userVO, currentUserOrg, switchContextDTO.getFiscal(),
                StpUtil.getTokenValue(), userOrgDao.selectByUserId(userId), snapshot);
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
                                             Integer fiscal, LoginPermissionSnapshotVO snapshot) {
        loginPermissionCacheRefresher.writeCache(userVO, currentUserOrg, fiscal, snapshot);
    }

    private LoginInfo buildLoginInfo(UserVO userVO,
                                     UserOrgVO currentUserOrg,
                                     Integer fiscal,
                                     String token,
                                     List<UserOrgVO> userOrgList,
                                     LoginPermissionSnapshotVO snapshot) {
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setUserId(userVO.getUserId());
        loginInfo.setUserName(userVO.getUserName());
        loginInfo.setFiscal(String.valueOf(fiscal));
        loginInfo.setTenantId(currentUserOrg.getTenantId());
        loginInfo.setTenantCode(currentUserOrg.getTenantCode());
        loginInfo.setTenantName(currentUserOrg.getTenantName());
        loginInfo.setOrgId(currentUserOrg.getOrgId());
        loginInfo.setOrgCode(currentUserOrg.getOrgCode());
        loginInfo.setOrgName(currentUserOrg.getOrgName());
        loginInfo.setIsDefaultPwd(resolveIsDefaultPwd(userVO.getIsModify()));
        loginInfo.setUserOrgList(userOrgList);
        loginInfo.setRoleList(snapshot.getRoleList());
        loginInfo.setMenuList(snapshot.getMenuList());
        loginInfo.setRouterList(snapshot.getRouterList());
        loginInfo.setResourceList(snapshot.getResourceList());
        loginInfo.setPermissionList(snapshot.getPermissionList());
        loginInfo.setPermissionSnapshot(snapshot);
        loginInfo.setToken(token);
        return loginInfo;
    }

    private Integer resolveIsDefaultPwd(Integer isModify) {
        if (isModify == null) {
            return null;
        }
        return isModify == 0 ? 1 : 0;
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
