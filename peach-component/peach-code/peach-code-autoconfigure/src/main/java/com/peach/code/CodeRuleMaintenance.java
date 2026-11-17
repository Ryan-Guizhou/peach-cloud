package com.peach.code;

/**
 * 业务编码规则的受控管理契约。
 *
 * <p>该契约只向服务端受信任的管理流程开放，不提供匿名 HTTP 端点。
 * 调用方必须完成平台管理员或租户管理员的范围鉴权。
 * 已发放编号的规则禁止变更格式，水位只能前进，禁止重置历史发号记录。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:30
 */
public interface CodeRuleMaintenance {

    /**
     * 启用或停用既有规则，不能通过停用重建规则或复位水位。
     *
     * @param namespace 已存在的租户命名空间，平台使用 PLATFORM
     * @param prefix 业务编号类型
     * @param enabled 是否启用
     * @param expectedVersion 预期规则配置版本
     * @param operatorRefId 经服务端权限验证的操作者物理 ID
     */
    void changeStatus(String namespace, String prefix, boolean enabled, long expectedVersion, String operatorRefId);

    /**
     * 仅在水位尚为零时变更规则格式。发出首个编号后格式不可修改。
     *
     * @param namespace 租户或平台命名空间
     * @param prefix 已存在的编码类型
     * @param format 受控格式，前缀必须与规则编码类型一致
     * @param expectedVersion 预期规则版本
     * @param operatorRefId 经服务端权限验证的操作者物理 ID
     */
    void changeFormat(String namespace, String prefix, CodeFormat format, long expectedVersion, String operatorRefId);

    /**
     * 为历史数据迁移向前校准水位，绝不允许回退。
     *
     * @param namespace 租户或平台命名空间
     * @param prefix 已存在的编码类型
     * @param watermark 最小安全水位，必须大于或等于数据库当前值
     * @param expectedVersion 预期规则版本
     * @param operatorRefId 经服务端权限验证的操作者物理 ID
     */
    void advanceWatermark(String namespace, String prefix, long watermark, long expectedVersion, String operatorRefId);
}
