package com.peach.common.constant;

import java.nio.charset.StandardCharsets;

/**
 * 通用公共常量类。
 *
 * <p>定义系统中全局通用的逻辑标识、符号分隔符、字符集、HTTP 协议头、排序方式及基础配置常量。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:50
 */
public final class PubCommonConst {

    private PubCommonConst() {
        throw new IllegalStateException("Utility class");
    }

    // ==========================================
    // 1. 逻辑与开关常量 (0/1, true/false)
    // ==========================================

    /**
     * 逻辑是/成功 (数值类型: 1)
     */
    public static final Integer LOGIC_TRUE = 1;

    /**
     * 逻辑否/失败 (数值类型: 0)
     */
    public static final Integer LOGIC_FLASE = 0;

    /**
     * 逻辑是/成功 (字符串类型: "1")
     */
    public static final String STR_LOGIC_TRUE = "1";

    /**
     * 逻辑否/失败 (字符串类型: "0")
     */
    public static final String STR_LOGIC_FLASE = "0";

    /**
     * 字符串 "true"
     */
    public static final String STR_TRUE = "true";

    /**
     * 字符串 "false"
     */
    public static final String STR_FALSE = "false";

    /**
     * Boolean 封装类 TRUE
     */
    public static final Boolean TRUE = true;

    /**
     * Boolean 封装类 FALSE
     */
    public static final Boolean FALSE = false;


    // ==========================================
    // 2. 基础符号与分隔符常量
    // ==========================================

    /**
     * 空字符串
     */
    public static final String EMPTY = "";

    /**
     * 单个空格
     */
    public static final String SPACE = " ";

    /**
     * 逗号分隔符
     */
    public static final String COMMA = ",";

    /**
     * 冒号分隔符
     */
    public static final String SEPARATOR_COLON = ":";

    /**
     * 竖线分隔符
     */
    public static final String SEPARATOR_VERTICAL_LINE = "|";

    /**
     * 下划线分隔符
     */
    public static final String UNDER_LINE = "_";

    /**
     * 中划线/连字符
     */
    public static final String DASH = "-";

    /**
     * 正斜杠
     */
    public static final String SLASH = "/";

    /**
     * #
     */
    public static final String SHARP = "#";

    /**
     * 句点/点分隔符
     */
    public static final String DOT = ".";

    /**
     * 系统文件路径分隔符 (Linux 为 '/', Windows 为 '\')
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * 不限制/无限额度标识 (0)
     */
    public static final Integer UN_LIMIT = 0;

    /**
     * 通配符常量，表示匹配所有（如 CORS 来源或路径匹配）。
     * <p>注意：在允许携带凭证的 CORS 场景中，此值已被安全策略禁止使用。</p>
     */
    public static final String WILDCARD_ALL = "*";

    /**
     * 逗号分隔符常量，用于拆分多值配置（如 IP 黑名单、允许来源列表等）。
     */
    public static final String SEPARATOR_COMMA = ",";


    /**
     * "null" 文本字符串常量（值为 {@code "null"}）。
     *
     * <p>通常用于针对前端传参、JSON 反序列化或数据库边界值的容错判断与兼容处理。</p>
     */
    public static final String STR_NULL_TEXT = "null";

    /**
     * 空字符串引用常量（值为 {@code null}）。
     *
     * <p>用于表示默认未初始化、缺失值或显式的空对象状态。</p>
     */
    public static final String NULL_STRING = null;


    // ==========================================
    // 3. 字符集编码常量
    // ==========================================

    /**
     * 字符集 UTF-8
     */
    public static final String UTF_8 = StandardCharsets.UTF_8.name();

    /**
     * 字符集 GBK
     */
    public static final String GBK = "GBK";

    /**
     * 字符集 ISO-8859-1
     */
    public static final String ISO_8859_1 = StandardCharsets.ISO_8859_1.name();


    // ==========================================
    // 4. HTTP 请求方法与 Header 常量
    // ==========================================

    /**
     * HTTP 请求方式：GET
     */
    public static final String REQUEST_GET = "GET";

    /**
     * HTTP 请求方式：POST
     */
    public static final String REQUEST_POST = "POST";

    /**
     * HTTP 请求方式：PUT
     */
    public static final String REQUEST_PUT = "PUT";

    /**
     * HTTP 请求方式：DELETE
     */
    public static final String REQUEST_DELETE = "DELETE";

    /**
     * HTTP 请求头：User-Agent
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * HTTP 请求头：Authorization 身份认证
     */
    public static final String KNIFE4J_AUTHORIZATION_HEADER = "Authorization";

    /**
     * HTTP 鉴权 Token 前缀 Bearer
     */
    public static final String BEARER_TYPE = "Bearer ";

    /**
     * HTTP 请求头：Content-Type
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 常用 Content-Type：application/json;charset=UTF-8
     */
    public static final String CONTENT_TYPE = "application/json";

    /**
     * 常用 Content-Type：application/json;charset=UTF-8 (带字符集)
     */
    public static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    /**
     * 常用 Content-Type：表单提交
     */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 常用 Content-Type：文件上传 multipart/form-data
     */
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";


    // ==========================================
    // 5. 常见 HTTP / 业务状态码
    // ==========================================

    /**
     * HTTP 响应成功状态码：200
     */
    public static final int HTTP_STATUS_OK = 200;

    /**
     * HTTP 未授权状态码：401
     */
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;

    /**
     * HTTP 无权限/禁止访问状态码：403
     */
    public static final int HTTP_STATUS_FORBIDDEN = 403;

    /**
     * HTTP 资源未找到状态码：404
     */
    public static final int HTTP_STATUS_NOT_FOUND = 404;

    /**
     * HTTP 服务器内部错误状态码：500
     */
    public static final int HTTP_STATUS_ERROR = 500;


    // ==========================================
    // 6. 排序与查询常量
    // ==========================================

    /**
     * 排序方式：降序 (desc)
     */
    public static final String ORDER_TYPE_DESC = "desc";

    /**
     * 排序方式：升序 (asc)
     */
    public static final String ORDER_TYPE_ASC = "asc";


    // ==========================================
    // 7. 系统策略与验证类型
    // ==========================================

    /**
     * 限流策略：直接拒绝
     */
    public static final String REFUSE = "REFUSE";

    /**
     * 限流策略：平滑限流/排队等待
     */
    public static final String SMOOTH = "SMOOTH";

    /**
     * 验证方式：1. 滑块/图形验证
     */
    public static final Integer VALIDATE_TYPE_IMAGE = 1;

    /**
     * 验证方式：2. 邮件验证
     */
    public static final Integer VALIDATE_TYPE_EMAIL = 2;

    /**
     * 验证方式：3. 短信验证
     */
    public static final Integer VALIDATE_TYPE_SMS = 3;


    /**
     * 未知
     */
    public static final String UNKNOWN = "unknown";
}
