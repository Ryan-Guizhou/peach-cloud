-- #258: 存量 MySQL 8.4 的 PEACH_USER 乐观锁/删除操作者一次性升级。
-- 必须先完成 #220 的 PASSWORD_VERSION 迁移，并在 DBA 备份后人工执行。
-- 禁止在应用启动时自动运行；新库只使用最新单表/聚合 DDL。
-- 旧数据版本从 0 起步，历史删除操作人不允许猜测回填。
-- 记录执行 SHA、实际结构、变更窗口及回滚证据到 #211。

ALTER TABLE PEACH_USER
    ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0 COMMENT '全局用户资料乐观并发版本' AFTER PASSWORD_VERSION,
    ADD COLUMN DELETED_BY_REF_ID VARCHAR(32) NULL COMMENT '删除操作人可信成员物理ID' AFTER DELETED_AT;
