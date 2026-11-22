package com.peach.log.util;

import com.peach.log.annoation.UserOperLog;
import com.peach.log.vo.UserOperLogVO;
import com.peach.common.response.Response;
import com.peach.common.util.DateUtil;
import com.peach.satoken.context.SecurityContextHolder;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.BeanWrapperImpl;

/**
 * 采集可信身份与操作白名单，不执行请求对象SpEL，不保存完整请求、响应或异常消息。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
public final class TransferUtil {
    private TransferUtil() { }
    /** 保留既有调用签名，委托白名单采集。 */
    public static UserOperLogVO transferToOperLog(MethodInvocation invocation, UserOperLog annotation, long elapsed, Response response) {
        return transferToOperLog(invocation, annotation, elapsed, response, null);
    }
    /** 在请求线程捕获上下文，异步写入线程不再读取线程本地身份。 */
    public static UserOperLogVO transferToOperLog(MethodInvocation invocation, UserOperLog annotation, long elapsed, Object result, Throwable failure) {
        var actor = SecurityContextHolder.get();
        if (actor == null || actor.getTenantId() == null || actor.getOrgId() == null) return null;
        var record = new UserOperLogVO();
        record.setId(UUID.randomUUID().toString().replace("-", ""));
        record.setTenantId(actor.getTenantId()); record.setOrgId(actor.getOrgId());
        record.setCreatorCode(bound(actor.getUserCode(), 50)); record.setCreatorName(bound(actor.getUserName() == null ? actor.getUserCode() : actor.getUserName(), 100));
        record.setModuleCode(annotation.moduleCode().getModuleCode());
        record.setOptTypeCode(annotation.optType().getOptTypeCode());
        record.setOptLevel(annotation.optLevel().getLogLevelCode());
        String label = annotation.optContent();
        // Legacy annotations often interpolate the complete DTO. Keep the static label only.
        if (label.contains("#")) {
            int start = label.indexOf('\''); int end = start < 0 ? -1 : label.indexOf('\'', start + 1);
            label = end > start ? label.substring(start + 1, end).split("[,，:：\\[]", 2)[0] : "";
        }
        if (label.isBlank()) label = annotation.moduleCode().getModuleName() + "：" + annotation.optType().getOptTypeName();
        List<String> targets = new ArrayList<>();
        List<String> fields = List.of("roleId", "roleCode", "routerId", "routerCode", "userId", "funcId", "funcCode", "resourceId");
        Object[] arguments = invocation.getArguments();
        if (arguments != null) for (int index = 0; index < arguments.length; index++) {
            Object argument = arguments[index];
            if (argument == null) continue;
            if (argument instanceof String value) {
                String name = index < invocation.getMethod().getParameterCount() ? invocation.getMethod().getParameters()[index].getName() : "";
                if (fields.contains(name) && value.matches("[A-Za-z0-9_-]{1,128}")) targets.add(name + "=" + value);
            } else if (!(argument instanceof java.util.Map<?,?>)) {
                BeanWrapperImpl bean = new BeanWrapperImpl(argument);
                for (String field : fields) if (bean.isReadableProperty(field)) {
                    Object value = bean.getPropertyValue(field);
                    if (value instanceof String text && text.matches("[A-Za-z0-9_-]{1,128}")) targets.add(field + "=" + text);
                }
            }
        }
        record.setOptContent(bound(label + (targets.isEmpty() ? "" : "（" + String.join(", ", targets) + "）"), 4000)); record.setCreateTime(DateUtil.nowTime());
        record.setExecutionTime(Math.max(0, elapsed));
        boolean successful = failure == null && (!(result instanceof Response response) || response.isSuccess());
        record.setIsSuccess(successful ? "Y" : "N");
        record.setErrorMsg(successful ? null : failure == null ? "业务处理失败" : failure.getClass().getSimpleName());
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            record.setRequestMethod(bound(request.getMethod(), 10)); record.setRequestUri(bound(request.getRequestURI(), 500));
            record.setPrivateIp(bound(request.getRemoteAddr(), 50)); record.setPublicIp(bound(request.getRemoteAddr(), 50));
        }
        return record;
    }
    private static String bound(String value, int length) {
        if (value == null) return null;
        String clean = value.replaceAll("[\\p{Cntrl}]", "");
        return clean.substring(0, Math.min(clean.length(), length));
    }
}
