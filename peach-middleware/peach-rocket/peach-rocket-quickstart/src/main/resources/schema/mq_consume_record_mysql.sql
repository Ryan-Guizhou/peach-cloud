CREATE TABLE mq_consume_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    idempotent_key VARCHAR(256) NOT NULL COMMENT '幂等键',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费者组',
    topic VARCHAR(128) DEFAULT NULL COMMENT 'RocketMQ topic',
    tag VARCHAR(128) DEFAULT NULL COMMENT 'RocketMQ tag',
    business_key VARCHAR(128) DEFAULT NULL COMMENT '业务 key',
    message_id VARCHAR(128) DEFAULT NULL COMMENT '消息 ID',
    status VARCHAR(32) NOT NULL COMMENT '状态：PROCESSING、SUCCESS、FAILED',
    consume_count INT NOT NULL DEFAULT 0 COMMENT '消费次数',
    last_error VARCHAR(1000) DEFAULT NULL COMMENT '最近一次失败原因',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotent_group (idempotent_key, consumer_group),
    KEY idx_message_id (message_id),
    KEY idx_status_update (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ 消费记录表';
