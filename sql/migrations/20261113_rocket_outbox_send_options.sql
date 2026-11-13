-- 在部署含 Outbox 发送选项持久化的新版本前执行一次；旧数据继续以默认超时发送。
-- 现有 SHARDING_KEY / DELAY_LEVEL / HEADERS / SEND_MODE 字段无需破坏性迁移。
ALTER TABLE MQ_OUTBOX_EVENT
    ADD COLUMN SEND_TIMEOUT_MS BIGINT NULL COMMENT '本次发送超时毫秒数，NULL 使用默认超时';
