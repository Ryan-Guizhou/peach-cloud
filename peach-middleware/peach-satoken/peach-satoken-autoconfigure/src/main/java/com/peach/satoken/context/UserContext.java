package com.peach.satoken.context;


import java.io.Serializable;

/**
 * 用户上下文信息类。
 * <p>
 * 用于在系统各层之间传递当前登录用户的基本信息、组织信息、租户信息以及请求上下文相关属性。
 * 该类实现了 {@link Serializable} 接口，支持跨线程、跨会话或分布式环境下的序列化传输。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:22
 */
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识（通常为主键ID）
     * <p>一般为数据库自增ID或UUID，不可为空，用于唯一确定一个用户。</p>
     */
    private String userId;

    /**
     * 用户编码（工号/登录账号）
     * <p>用于业务系统间交互或作为外部系统的用户唯一标识，通常具有业务含义。</p>
     */
    private String userCode;

    /**
     * 用户姓名（显示名称）
     * <p>用于界面展示、日志记录及审批流中显示操作人姓名。</p>
     */
    private String userName;

    /**
     * 租户ID
     * <p>在多租户架构中标识当前用户所属租户，用于数据隔离和权限控制。</p>
     */
    private String tenantId;

    /**
     * 租户名称
     * <p>租户的显示名称，便于业务操作时展示租户信息。</p>
     */
    private String tenantName;

    /**
     * 组织机构ID
     * <p>当前用户所属部门/机构的唯一标识，用于数据权限过滤和报表统计。</p>
     */
    private String orgId;

    /**
     * 组织机构编码
     * <p>组织机构的业务编码，常用于与外部系统对接或层级关系的计算。</p>
     */
    private String orgCode;

    /**
     * 组织机构名称
     * <p>显示用的机构全称，用于页面展示及审批流节点信息。</p>
     */
    private String orgName;

    /**
     * 会计期间（账套/财务年度）
     * <p>标识当前用户操作的财务年度或会计期间，用于财务模块数据隔离。</p>
     */
    private String fiscal;

    /**
     * 当前请求路径（URL）
     * <p>用于日志记录、权限校验或审计追踪，记录用户访问的具体接口地址。</p>
     */
    private String requestPath;

    /**
     * 国际化语言代码
     * <p>例如：zh_CN、en_US，用于后端返回多语言消息时的本地化处理。</p>
     */
    private String lang;

    /**
     * 上下文版本号
     * <p>用于缓存管理或乐观锁控制，当用户上下文信息发生变更时递增此版本号。</p>
     */
    private long contextVersion;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getFiscal() {
        return fiscal;
    }

    public void setFiscal(String fiscal) {
        this.fiscal = fiscal;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getContextVersion() {
        return contextVersion;
    }

    public void setContextVersion(long contextVersion) {
        this.contextVersion = contextVersion;
    }

    // 可选：为了完整性，可重写 toString、equals 和 hashCode（根据实际需求决定是否添加）
    @Override
    public String toString() {
        return "UserContext{" +
                "userId='" + userId + '\'' +
                ", userCode='" + userCode + '\'' +
                ", userName='" + userName + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", tenantName='" + tenantName + '\'' +
                ", orgId='" + orgId + '\'' +
                ", orgCode='" + orgCode + '\'' +
                ", orgName='" + orgName + '\'' +
                ", fiscal='" + fiscal + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", lang='" + lang + '\'' +
                ", contextVersion=" + contextVersion +
                '}';
    }
}
