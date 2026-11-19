-- #220: Execute once on the actual production schema using a reviewed DBA change.
-- Ensure there are no active uncoordinated Auth/Gateway instances relying on old sessions.
-- Existing sessions intentionally lack passwordVersion and will be rejected after deployment.
-- Deploy before any new Auth binary that queries PEACH_USER.PASSWORD_VERSION.
ALTER TABLE PEACH_USER
    ADD COLUMN PASSWORD_VERSION BIGINT NOT NULL DEFAULT 0
    COMMENT '全局密码凭证版本，递增后所有旧Token失效'
    AFTER PASSWORD;

-- Production rollout must invalidate existing Sa-Token sessions and verify Gateway
-- on-line authorization, Redis unavailable behavior, password recovery and multi-device.
-- No password hashes, reset tokens or user data are written in this migration.
