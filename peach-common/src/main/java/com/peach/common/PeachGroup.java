package com.peach.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 * @Description 基础分组规则 - 定义增删改查四种操作分组
 *               用于 @Validated 注解指定校验规则分组
 */
public class PeachGroup {

    /** 新增操作分组 */
    public interface InsertGroup {
    }

    /** 更新操作分组 */
    public interface UpdateGroup {
    }

    /** 删除操作分组 */
    public interface DeleteGroup {
    }

    /** 查询操作分组 */
    public interface QueryGroup {
    }
}