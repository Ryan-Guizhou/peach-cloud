package com.peach.common.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:50
 */
public interface PubCommonConst {

    /**
     * 当前文件路径
     */
    String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
     * 逻辑是
     */
    Integer LOGIC_TRUE = 1;

    /**
     * 逻辑否
     */
    Integer LOGIC_FLASE = 0;

    /**
     * 逻辑是(字符串类型)
     */
    String STR_LOGIC_TRUE = "1";

    /**
     * 逻辑否(字符串类型)
     */
    String STR_LOGIC_FLASE = "0";


    /**
     * 字符集 UTF-8
     */
    String UTF_8 = "UTF-8";

    /**
     * 字符集 GBK
     */
    String GBK = "GBK";

    /**
     * 字符串true
     */
    String STR_TRUE = "true";

    /**
     * 字符串 false
     */
    String STR_FALSE = "false";

    /**
     * boolean true
     */
    Boolean TRUE = true;

    /**
     * boolean false
     */
    Boolean FALSE = false;

    /**
     * user-agent
     */
    String USER_AGENT = "User-Agent";

    /**
     * 排序方式 降序
     */
    String ORDER_TYPE_DESC = "desc";

    /**
     * 排序方式 升序
     */
    String ORDER_TYPE_ASC = "asc";

    /**
     * 请求方式 GET
     */
    String REQUEST_GET = "GET";

    /**
     * 请求方式 POST
     */
    String REQUEST_POST = "POST";

    /**
     * CONTENT_TYPE
     */
    String CONTENT_TYPE = "application/json";

    /**
     * 限流策略,直接拒绝
     */
    String REFUSE = "REFUSE";

    /**
     * 平滑限流
     */
    String SMOOTH = "SMOOTH";


    /**
     * 验证方式 1、滑块验证
     */
    Integer VALIDATE_TYPE_IMAGE = 1;

    /**
     * 验证方式 2、邮件验证
     */
    Integer VALIDATE_TYPE_EMAIL = 2;

    /**
     * 通配符常量，表示匹配所有（如 CORS 来源或路径匹配）。
     * <p>注意：在允许携带凭证的 CORS 场景中，此值已被安全策略禁止使用。</p>
     */
    String WILDCARD_ALL  = "*";

    /**
     * 逗号分隔符常量，用于拆分多值配置（如 IP 黑名单、允许来源列表等）。
     */
    String SEPARATOR_COMMA = ",";
}
