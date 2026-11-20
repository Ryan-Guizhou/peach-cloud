package com.peach.auth.util;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户操作审计安全转换工具。
 *
 * <p>只记录静态动作、服务端已验证的物理身份与不包含业务内容的执行结果。
 * 严禁将请求参数、SpEL 展开的 DTO、响应载荷、Token 或动态异常消息写入审计。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 13:50
 */
public final class TransferUtil {

    private static final int MAX_ACTION_LENGTH = 256;
    private static final String FAILED_MESSAGE = "Operation failed";

    private TransferUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 转换为只包含安全白名单元数据的审计实体。
     *
     * @param invocation 拦截的方法，保留原公共方法签名，不读取参数对象
     * @param operLog 已验证的服务器端方法注解
     * @param executionTime 已耗费时间，单位为毫秒
     * @param response 执行结果；抛异常时为 null
     * @return 不包含参数、响应载荷或敏感异常文本的审计记录
     */
    public static UserOperLogVO transferToOperLog(MethodInvocation invocation, UserOperLog operLog,
                                                  long executionTime, Response response) {
        HttpServletRequest request = getHttpServletRequest();
        UserLogEnum.Module module = operLog.moduleCode();
        UserLogEnum.OptType operation = operLog.optType();
        UserLogEnum.LogLevel level = operLog.optLevel();
        String moduleCode = module == null ? StringUtil.EMPTY : module.getModuleCode();
        String operationCode = operation == null ? StringUtil.EMPTY : operation.getOptTypeCode();
        boolean success = response != null && Boolean.TRUE.equals(response.isSuccess());

        UserOperLogVO logRecord = new UserOperLogVO();
        logRecord.setId(UniqueIdFacade.nextId());
        logRecord.setOptTypeCode(operationCode);
        logRecord.setModuleCode(moduleCode);
        logRecord.setOptContent(safeAction(operLog.optContent(), moduleCode, operationCode));
        logRecord.setCreateTime(DateUtil.nowTime());
        if (level != null) {
            logRecord.setOptLevel(level.getLogLevelCode());
        }
        logRecord.setExecutionTime(Math.max(0L, executionTime));
        logRecord.setIsSuccess(success ? PubCommonConst.STR_TRUE : PubCommonConst.STR_FALSE);
        if (!success) {
            logRecord.setErrorMsg(FAILED_MESSAGE);
        }
        if (request != null) {
            logRecord.setRequestMethod(request.getMethod());
            logRecord.setRequestUri(request.getRequestURI());
            DiviceInfo device = getDiviceInfo(request);
            logRecord.setPrivateIp(device.getPrivateIp());
            logRecord.setPublicIp(device.getPublicIp());
            logRecord.setTenantRefId(trustedPhysicalId(request, UserOperLogTrustedContext.TENANT_REF_ID));
            logRecord.setAppRefId(trustedPhysicalId(request, UserOperLogTrustedContext.APP_REF_ID));
            logRecord.setMemberRefId(trustedPhysicalId(request, UserOperLogTrustedContext.MEMBER_REF_ID));
        }
        return logRecord;
    }

    /**
     * 仅保留短的静态动作描述；所有动态 SpEL 和拼接表达式均回退为操作编码。
     */
    private static String safeAction(String raw, String module, String operation) {
        if (StringUtil.isBlank(raw) || raw.length() > MAX_ACTION_LENGTH
                || raw.indexOf('#') >= 0 || raw.indexOf('+') >= 0
                || raw.indexOf('{') >= 0 || raw.indexOf('}') >= 0) {
            return module + ":" + operation;
        }
        String content = raw.trim();
        if (content.length() > 1 && content.startsWith("'") && content.endsWith("'")) {
            return content.substring(1, content.length() - 1);
        }
        return content;
    }

    /**
     * 只读取 Servlet 属性，不访问 HTTP Header、表单参数或旧 Redis 用户缓存。
     */
    private static String trustedPhysicalId(HttpServletRequest request, String attribute) {
        Object value = request.getAttribute(attribute);
        if (!(value instanceof String id) || StringUtil.isBlank(id) || id.length() > 32) {
            return null;
        }
        return id;
    }

    private static HttpServletRequest getHttpServletRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    /**
     * 操作设备的最小必要信息，禁止存储完整 User-Agent 和请求报文。
     */
    private static DiviceInfo getDiviceInfo(HttpServletRequest request) {
        DiviceInfo device = new DiviceInfo();
        String address = IpUtil.getIpAddr(request);
        device.setPrivateIp(address);
        device.setPublicIp(address);
        return device;
    }

    /**
     * 兼容原有操作审计设备视图，不扩展敏感数据采集。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/20 13:50
     */
    @Data
    public static class DiviceInfo {
        private String os;
        private String privateIp;
        private String publicIp;
        private String device;
        private String browser;
    }
}
