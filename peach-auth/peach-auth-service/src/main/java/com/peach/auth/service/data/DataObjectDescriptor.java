package com.peach.auth.service.data;

/**
 * 由业务服务从真实数据库记录构造的数据对象权属描述。
 *
 * <p>租户、应用、自然年度、对象、所有者与机构均来自服务端事实源，
 * 不得直接复用浏览器请求体中的值。删除记录永不参与普通授权决策。</p>
 *
 * @param tenantRefId 对象所属租户物理主键
 * @param appRefId 对象所属应用物理主键
 * @param year 对象数据所属自然年度
 * @param objectRefId 对象物理主键
 * @param ownerUserRefId 所有者用户物理主键，可以为空
 * @param orgRefId 对象所属机构物理主键，可以为空
 * @param deleted 是否已逻辑删除
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
public record DataObjectDescriptor(String tenantRefId, String appRefId, int year,
                                   String objectRefId, String ownerUserRefId,
                                   String orgRefId, boolean deleted) {
}
