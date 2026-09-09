package com.peach.common.audit;

import com.peach.common.PeachDO;

import java.util.Objects;

/**
 * PeachDO 审计字段填充工具。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class PeachAuditFiller {

    private PeachAuditFiller() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 从当前审计上下文填充创建字段、租户及组织信息。
     *
     * @param entity 目标实体
     */
    public static void fillCreateFromContext(PeachDO entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        fillCreate(entity, AuditSnapshot.fromContext());
    }

    /**
     * 按快照填充创建字段、租户及组织信息。
     *
     * @param entity   目标实体
     * @param snapshot 审计快照
     */
    public static void fillCreate(PeachDO entity, AuditSnapshot snapshot) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        entity.fillCreateTime(snapshot.userId());
        entity.fillTenantOrg(snapshot.tenantId(), snapshot.orgId());
    }

    /**
     * 从当前审计上下文填充修改字段。
     *
     * @param entity 目标实体
     */
    public static void fillModifyFromContext(PeachDO entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        entity.fillModifyTime(AuditContext.currentUserId().orElse(null));
    }

    /**
     * 按指定操作人填充修改字段。
     *
     * @param entity     目标实体
     * @param modifierId 修改人 ID
     */
    public static void fillModify(PeachDO entity, String modifierId) {
        Objects.requireNonNull(entity, "entity must not be null");
        entity.fillModifyTime(modifierId);
    }
}
