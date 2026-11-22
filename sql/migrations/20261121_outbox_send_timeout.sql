-- Upgrade an existing Outbox table to the current JdbcMqOutboxStore contract.
-- Select the target Peach database before running. Compatible with MySQL 5.7.
-- Existing rows retain NULL, which uses the default send timeout.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @PEACH_OUTBOX_TIMEOUT_DDL = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE MQ_OUTBOX_EVENT ADD COLUMN SEND_TIMEOUT_MS BIGINT NULL COMMENT ''本次发送超时毫秒数，NULL 使用默认超时'' AFTER DELAY_LEVEL',
        'SELECT ''SEND_TIMEOUT_MS already exists'' AS MIGRATION_STATUS')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'MQ_OUTBOX_EVENT'
      AND COLUMN_NAME = 'SEND_TIMEOUT_MS'
);
PREPARE PEACH_OUTBOX_TIMEOUT_STATEMENT FROM @PEACH_OUTBOX_TIMEOUT_DDL;
EXECUTE PEACH_OUTBOX_TIMEOUT_STATEMENT;
DEALLOCATE PREPARE PEACH_OUTBOX_TIMEOUT_STATEMENT;
