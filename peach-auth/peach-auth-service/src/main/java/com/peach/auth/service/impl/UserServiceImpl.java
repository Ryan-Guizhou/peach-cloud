package com.peach.auth.service.impl;

import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.github.pagehelper.PageInfo;
import com.peach.auth.LoginInfo;
import com.peach.auth.common.RsaPasswordUtil;
import com.peach.auth.common.SensitiveFieldCipher;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.dto.RegisterDTO;
import com.peach.auth.dto.SwitchContextDTO;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IUserService;
import com.peach.auth.service.SaasLoginSnapshotService;
import com.peach.auth.service.LoginLockService;
import com.peach.auth.service.support.LoginInitConfigSupport;
import com.peach.auth.service.support.PasswordHashSupport;
import com.peach.auth.service.support.LoginTargetSelector;
import com.peach.auth.service.support.UserSensitiveFieldSupport;
import com.peach.auth.vo.LoginInitVO;
import com.peach.auth.vo.LoginTargetVO;
import com.peach.auth.vo.LoginLockStatusVO;
import com.peach.auth.vo.LoginPermissionSnapshotVO;
import com.peach.auth.vo.UserVO;
import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.service.CaptchaService;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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

    private final AuthBusinessIdGenerator authBusinessIdGenerator;

    private final UserDao userDao;

    private final TenantMemberDao tenantMemberDao;

    private final AuthenticatedInvitationService invitationService;

    private final TenantSessionService tenantSessionService;

    private final SaasLoginSnapshotService saasLoginSnapshotService;

    private final CaptchaService captchaService;

    private final LoginLockService loginLockService;

    private final LoginInitConfigSupport loginInitConfigSupport;

    private final PasswordHashSupport passwordHashSupport;


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
        Response captchaResponse = verifyLoginCaptcha(
                loginDTO.getCaptchaVerification(), loginDTO.getClientUid());
        if (!captchaResponse.isSuccess()) {
            return captchaResponse;
        }

        String username = loginDTO.getUsername().trim();
        LoginLockStatusVO lockStatus = loginLockService.checkLock(username);
        if (lockStatus.isLocked()) {
            return Response.fail(formatLockMessage(lockStatus));
        }

        String decryptedPassword;
        try {
            decryptedPassword = RsaPasswordUtil.decrypt(loginDTO.getPassword());
        } catch (RuntimeException exception) {
            log.warn("Password decryption failed");
            return Response.fail("用户名或密码错误");
        } catch (Exception exception) {
            log.warn("Password decryption failed");
            return Response.fail("用户名或密码错误");
        }
        if (StringUtil.isBlank(decryptedPassword)) {
            return Response.fail("用户名或密码错误");
        }

        UserVO user = userDao.login(username);
        boolean passwordMatches = passwordHashSupport.matches(
                decryptedPassword, user == null ? null : user.getPassword());
        if (user == null || !passwordMatches || StringUtil.isBlank(user.getId())) {
            log.warn("Global account authentication failed");
            LoginLockStatusVO failedStatus = loginLockService.recordFailure(username);
            return failedStatus.isLocked()
                    ? Response.fail(formatLockMessage(failedStatus))
                    : Response.fail("用户名或密码错误");
        }
        loginLockService.clearOnSuccess(username);
        UserSensitiveFieldSupport.decryptUserFields(user);

        // 只能在账号密码及验证码验证成功后消费本人邀请。
        // 临时全局登录令牌不返回给调用方，也绝不作为未授权的 SaaS 会话使用。
        if (StringUtil.isNotBlank(loginDTO.getInvitationRefId())) {
            try {
                invitationService.acceptAfterAuthentication(
                        user.getUserId(), user.getId(), loginDTO.getInvitationRefId());
            } catch (RuntimeException exception) {
                log.warn("Authenticated invitation acceptance rejected");
                return Response.fail("邀请无效、已过期或不可接受");
            }
        }

        // 先完成账号认证，再查询数据库有效租户成员与已开放应用。
        List<LoginTargetVO> targets = tenantMemberDao.selectLoginTargets(user.getId());
        LoginTargetVO selected;
        try {
            selected = LoginTargetSelector.select(
                    targets, loginDTO.getTenantRefId(), loginDTO.getAppRefId());
        } catch (SecurityException exception) {
            log.warn("Login tenant application selection rejected");
            return Response.fail("租户或应用不可用");
        }
        if (selected == null) {
            LoginInfo choices = new LoginInfo();
            choices.setUserId(user.getUserId());
            choices.setUserName(user.getUserName());
            choices.setRequiresTenantSelection(true);
            choices.setTenantOptions(targets);
            return Response.success(choices).setMsg("请选择租户和应用后重新提交登录");
        }

        StpUtil.login(user.getUserId(), new SaLoginParameter().setIsConcurrent(true).setIsShare(false));
        try {
            TenantSessionContext context = tenantSessionService.bindAfterAuthentication(
                    user.getId(), selected.getTenantRefId(), selected.getAppRefId());
            LoginInfo loginInfo = buildMemberLoginInfo(user, context, selected, targets);
            log.info("Tenant member login succeeded, memberRefId={}, tenantRefId={}",
                    context.memberRefId(), context.tenantRefId());
            return Response.success(loginInfo);
        } catch (RuntimeException exception) {
            // 对新建 Token 的绑定失败必须注销此凭证，不得留下无成员上下文的登录态。
            StpUtil.logout();
            log.warn("Tenant member login context rejected");
            return Response.fail("租户登录上下文验证失败");
        }
    }

    @Override
    public Response switchContext(SwitchContextDTO switchContextDTO) {
        try {
            TenantSessionContext context = tenantSessionService.switchTenant(
                    switchContextDTO.getTenantRefId(), switchContextDTO.getAppRefId());
            UserVO user = userDao.selectById(context.userRefId());
            if (user == null || !context.userRefId().equals(user.getId())) {
                return Response.fail("当前全局用户不可用");
            }
            List<LoginTargetVO> targets = tenantMemberDao.selectLoginTargets(context.userRefId());
            LoginTargetVO selected = LoginTargetSelector.select(
                    targets, context.tenantRefId(), context.appRefId());
            if (selected == null) {
                return Response.fail("目标租户或应用不可用");
            }
            UserSensitiveFieldSupport.decryptUserFields(user);
            LoginInfo loginInfo = buildMemberLoginInfo(user, context, selected, targets);
            log.info("Tenant context switched, memberRefId={}, tenantRefId={}",
                    context.memberRefId(), context.tenantRefId());
            return Response.success(loginInfo);
        } catch (SecurityException | IllegalStateException exception) {
            log.warn("Tenant context switch rejected");
            return Response.fail("租户切换失败或当前会话已失效");
        }
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
     * 基于已验证的成员物理关联生成登录视图；业务编号仅供展示。
     *
     * @param user 已认证全局用户
     * @param context 已验证的 Token 租户上下文
     * @param selected 已经校验的租户应用选项
     * @param targets 当前可访问的候选范围
     * @return 无过度授权的租户成员登录视图
     */
    private LoginInfo buildMemberLoginInfo(UserVO user, TenantSessionContext context,
                                           LoginTargetVO selected, List<LoginTargetVO> targets) {
        LoginPermissionSnapshotVO snapshot = saasLoginSnapshotService.requireSnapshot(context);
        LoginInfo info = new LoginInfo();
        info.setUserId(user.getUserId());
        info.setUserRefId(context.userRefId());
        info.setUserName(user.getUserName());
        info.setTenantId(selected.getTenantId());
        info.setTenantRefId(context.tenantRefId());
        info.setTenantName(selected.getTenantName());
        info.setMemberRefId(context.memberRefId());
        info.setAppRefId(context.appRefId());
        info.setAppId(selected.getAppId());
        info.setMemberVersion(context.memberVersion());
        info.setAuthVersion(context.authVersion());
        info.setRequiresTenantSelection(false);
        info.setTenantOptions(targets);
        info.setToken(StpUtil.getTokenValue());
        info.setIsDefaultPwd(resolveIsDefaultPwd(user.getIsModify()));
        info.setUserOrgList(List.of());
        info.setRoleList(snapshot.getRoleList());
        info.setMenuList(snapshot.getMenuList());
        info.setRouterList(snapshot.getRouterList());
        info.setResourceList(snapshot.getResourceList());
        info.setPermissionList(snapshot.getPermissionList());
        info.setPermissionSnapshot(snapshot);
        return info;
    }

    private Integer resolveIsDefaultPwd(Integer isModify) {
        if (isModify == null) {
            return null;
        }
        return isModify == 0 ? 1 : 0;
    }

    @Override
    public Response register(RegisterDTO registerDTO) {
        return Response.fail("账号注册尚未开放");
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
        userDO.setId(UniqueIdFacade.nextId());
        userDO.setUserId(authBusinessIdGenerator.next(AuthBusinessIdType.USER));
        if (StringUtil.isBlank(userDO.getPassword())) {
            userDO.setStatus("DISABLED");
        } else {
            userDO.setPassword(passwordHashSupport.encode(userDO.getPassword()));
        }
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
        if (StringUtil.isNotBlank(userDO.getPassword())) {
            userDO.setPassword(passwordHashSupport.encode(userDO.getPassword()));
        } else {
            userDO.setPassword(null);
        }
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
