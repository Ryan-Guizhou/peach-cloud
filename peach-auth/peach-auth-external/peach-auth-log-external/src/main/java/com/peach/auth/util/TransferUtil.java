package com.peach.auth.util;


import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.vo.UserOperLogVO;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.DateUtil;
import com.peach.common.util.IpUtil;
import com.peach.common.util.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2025/3/15 0:01
 */
@Slf4j
public class TransferUtil {


    /**
     * 转换成mysql入库实体
     * @param invocation
     * @param operLog
     * @param executionTime
     * @param response
     * @return
     */
    public static UserOperLogVO transferToOperLog(MethodInvocation invocation, UserOperLog operLog, long executionTime, Response response) {
        HttpServletRequest request = getHttpServletRequest();
        String requestMethod = request.getMethod();
        String requestURI = request.getRequestURI();
        String optContent = operLog.optContent();
        Object[] arguments = invocation.getArguments();
        optContent = getContent(arguments,optContent);

        UserLogEnum.Module moduleEnum = operLog.moduleCode();
        String moduleCode = moduleEnum == null ? StringUtil.EMPTY : moduleEnum.getModuleCode();

        UserLogEnum.OptType optTypeEnum = operLog.optType();
        String optTypeCode = optTypeEnum == null ? StringUtil.EMPTY : optTypeEnum.getOptTypeCode();

        UserLogEnum.LogLevel logLevel = operLog.optLevel();

        DiviceInfo diviceInfo = getDiviceInfo(request);
        UserOperLogVO userOperLogVO = new UserOperLogVO();
        userOperLogVO.setId(IDGeneratorUtil.UUID());
        userOperLogVO.setOptTypeCode(optTypeCode);
        userOperLogVO.setModuleCode(moduleCode);
        //从登录状态中获取
//        userOperLogVO.setCreatorCode();
//        userOperLogVO.setCreatorName();
//        userOperLogVO.setRoleCode();
        userOperLogVO.setOptContent(optContent);
        userOperLogVO.setCreateTime(DateUtil.nowTime());
        userOperLogVO.setOptLevel(logLevel.getLogLevelCode());
        userOperLogVO.setPrivateIp(diviceInfo.getPrivateIp());
        userOperLogVO.setPublicIp(diviceInfo.getPublicIp());
        userOperLogVO.setDevice(diviceInfo.getDevice());
        userOperLogVO.setBrowser(diviceInfo.getBrowser());
        userOperLogVO.setOs(diviceInfo.getOs());
        userOperLogVO.setExecutionTime(executionTime);
        userOperLogVO.setIsSuccess(response.isSuccess() + StringUtil.EMPTY);
        userOperLogVO.setRequestMethod(requestMethod);
        userOperLogVO.setRequestUri(requestURI);
        String errorMsg = response.isSuccess() == PubCommonConst.TRUE ? StringUtil.EMPTY : response.getMsg();
        userOperLogVO.setErrorMsg(errorMsg);
        userOperLogVO.setResponseData(JSONUtil.toJsonStr(response.getData()));
        return userOperLogVO;
    }


    /**
     * 解析spel
     * @param objects
     * @param content
     * @return
     */
    private static String getContent(Object[] objects,String content){
        try {
            if (content.contains("#p") && objects != null && objects.length > 0){
                SpelParse spelParse = SpelParse.create();
                for (int i = 0; i < objects.length; i ++) {
                    Object object = objects[i];
                    spelParse.setVariable("p" + i, JSON.toJSON(objects[i]));
                }
                return spelParse.parseExpression(content);
            }
            return content;
        }catch (Exception ex){
            log.error("spel parse failed"+ex.getMessage(),ex);
            return content;
        }
    }

    /**
     * 获取HttpServletRequest
     * @return
     */
    private static HttpServletRequest getHttpServletRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    /**
     * 获取Divice信息
     * @param request
     * @return
     */
    private static DiviceInfo getDiviceInfo(HttpServletRequest request) {
        DiviceInfo diviceInfo = new DiviceInfo();
        String userAgent = request.getHeader(PubCommonConst.USER_AGENT);

        // 使用 UserAgentUtils 解析 User-Agent
//        UserAgent agent = UserAgent.parseUserAgentString(userAgent);
//        Browser browser = agent.getBrowser();
//        OperatingSystem os = agent.getOperatingSystem();

        // 组装设备信息
//        diviceInfo.setOs(os.getName());
//        diviceInfo.setDevice(agent.getOperatingSystem().getName()); // 这里可以尝试解析设备名称
//        diviceInfo.setBrowser(browser.getName());
        diviceInfo.setPrivateIp(IpUtil.getIpAddr(request));
        diviceInfo.setPublicIp(IpUtil.getIpAddr(request));

        return diviceInfo;
    }

    @Data
    public static class DiviceInfo {

        /**
         * 操作系统
         */
        private String os;

        /**
         * 私网IP
         */
        private String privateIp;

        /**
         * 公网IP
         */
        private String publicIp;

        /**
         *
         */
        private String device;

        /**
         * 浏览器信息
         */
        private String browser;

    }

}
