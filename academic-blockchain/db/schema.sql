-- ============================================================================
-- 基于区块链的学术成果确权与共享平台 - 数据库 Schema
-- 论文 §3.4
-- 数据库：MySQL 8.x
-- ============================================================================

DROP DATABASE IF EXISTS academic_blockchain;
CREATE DATABASE academic_blockchain
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE academic_blockchain;

-- ----------------------------------------------------------------------------
-- 1. user 用户信息表（论文表 3.3）
-- ----------------------------------------------------------------------------
CREATE TABLE `user` (
  `user_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `username`       VARCHAR(64)  NOT NULL                COMMENT '用户名',
  `wallet_address` VARCHAR(64)  NOT NULL                COMMENT '区块链钱包地址（小写存储）',
  `phone`          VARCHAR(32)  DEFAULT NULL            COMMENT '联系电话',
  `email`          VARCHAR(128) DEFAULT NULL            COMMENT '电子邮件',
  `role`           VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_wallet` (`wallet_address`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB COMMENT='用户信息表';

-- ----------------------------------------------------------------------------
-- 2. achievement 成果信息表（论文表 3.4）
-- ----------------------------------------------------------------------------
CREATE TABLE `achievement` (
  `achievement_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成果编号',
  `user_id`        BIGINT       NOT NULL                COMMENT '所属用户编号',
  `name`           VARCHAR(255) NOT NULL                COMMENT '成果标题',
  `summary`        TEXT                                 COMMENT '成果摘要',
  `category`       VARCHAR(64)  DEFAULT NULL            COMMENT '成果类型',
  `status`         VARCHAR(32)  NOT NULL DEFAULT 'CREATED'
                                                       COMMENT 'CREATED / REGISTERED / SHARED',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`achievement_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='成果信息表';

-- ----------------------------------------------------------------------------
-- 3. achievement_file 成果文件表（论文表 3.5）
-- ----------------------------------------------------------------------------
CREATE TABLE `achievement_file` (
  `file_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件编号',
  `achievement_id` BIGINT       NOT NULL                COMMENT '成果编号',
  `file_name`      VARCHAR(255) NOT NULL                COMMENT '文件原始名称',
  `file_path`      VARCHAR(512) NOT NULL                COMMENT '链下存储路径',
  `file_type`      VARCHAR(32)  DEFAULT NULL            COMMENT '文件类型：pdf/word/...',
  `file_size`      BIGINT       NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
  `file_hash`      VARCHAR(80)  NOT NULL                COMMENT 'SHA-256 哈希（0x 前缀）',
  `upload_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_achievement` (`achievement_id`),
  KEY `idx_hash` (`file_hash`)
) ENGINE=InnoDB COMMENT='成果文件表';

-- ----------------------------------------------------------------------------
-- 4. achievement_record 成果存证记录表（论文表 3.6）
-- ----------------------------------------------------------------------------
CREATE TABLE `achievement_record` (
  `record_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '存证编号（链下自增）',
  `achievement_id`   BIGINT       NOT NULL                COMMENT '关联成果编号',
  `chain_record_id`  BIGINT       DEFAULT NULL            COMMENT '链上记录 ID',
  `file_hash`        VARCHAR(80)  NOT NULL                COMMENT '文件哈希',
  `metadata_hash`    VARCHAR(80)  DEFAULT NULL            COMMENT '元数据哈希',
  `owner_address`    VARCHAR(64)  NOT NULL                COMMENT '成果所有者钱包地址',
  `tx_hash`          VARCHAR(80)  DEFAULT NULL            COMMENT '链上交易哈希',
  `block_number`     BIGINT       DEFAULT NULL            COMMENT '所在区块高度',
  `record_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_file_hash` (`file_hash`),
  KEY `idx_achievement` (`achievement_id`),
  KEY `idx_chain_record` (`chain_record_id`)
) ENGINE=InnoDB COMMENT='成果存证记录表';

-- ----------------------------------------------------------------------------
-- 5. authorization_record 授权记录表（论文表 3.7）
-- ----------------------------------------------------------------------------
CREATE TABLE `authorization_record` (
  `authorization_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '授权编号',
  `achievement_id`   BIGINT       NOT NULL                COMMENT '成果编号',
  `chain_record_id`  BIGINT       NOT NULL                COMMENT '链上 record_id',
  `grantor_address`  VARCHAR(64)  NOT NULL                COMMENT '授权方钱包地址',
  `grantee_address`  VARCHAR(64)  NOT NULL                COMMENT '被授权方钱包地址',
  `permission_type`  VARCHAR(32)  NOT NULL DEFAULT 'READ' COMMENT '权限类型：READ / DOWNLOAD',
  `start_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权生效时间',
  `end_time`         DATETIME     DEFAULT NULL            COMMENT '授权结束时间（NULL=永久）',
  `status`           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
                                                          COMMENT 'ACTIVE / REVOKED / EXPIRED',
  `tx_hash`          VARCHAR(80)  DEFAULT NULL            COMMENT '授权链上交易哈希',
  `revoke_tx_hash`   VARCHAR(80)  DEFAULT NULL            COMMENT '撤销交易哈希',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`authorization_id`),
  KEY `idx_achievement` (`achievement_id`),
  KEY `idx_grantee` (`grantee_address`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='授权记录表';

-- ----------------------------------------------------------------------------
-- 6. verify_log 核验记录表
-- ----------------------------------------------------------------------------
CREATE TABLE `verify_log` (
  `verify_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '核验编号',
  `achievement_id`  BIGINT       DEFAULT NULL            COMMENT '匹配到的成果编号（未匹配为 NULL）',
  `verify_hash`     VARCHAR(80)  NOT NULL                COMMENT '待核验文件的哈希',
  `verify_result`   VARCHAR(16)  NOT NULL                COMMENT 'MATCHED / NOT_MATCHED',
  `chain_record_id` BIGINT       DEFAULT NULL            COMMENT '匹配到的链上 record_id',
  `owner_address`   VARCHAR(64)  DEFAULT NULL            COMMENT '匹配到的成果所有者地址',
  `record_time`     DATETIME     DEFAULT NULL            COMMENT '原成果存证时间（匹配成功时）',
  `operator_addr`   VARCHAR(64)  DEFAULT NULL            COMMENT '核验操作者钱包地址',
  `verify_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`verify_id`),
  KEY `idx_hash` (`verify_hash`),
  KEY `idx_operator` (`operator_addr`)
) ENGINE=InnoDB COMMENT='核验记录表';

-- ----------------------------------------------------------------------------
-- 7. operation_log 操作审计日志（论文 §2.2.1 角色 / 权限设计扩展）
--    由后端 AOP 切面 (OperationLogAspect) 自动写入所有
--    POST / PUT / DELETE 请求的执行记录，用于审计 + 越权检测。
-- ----------------------------------------------------------------------------
CREATE TABLE `operation_log` (
  `log_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志编号',
  `user_id`        BIGINT       DEFAULT NULL            COMMENT '操作用户编号（未登录时为 NULL）',
  `wallet_address` VARCHAR(64)  DEFAULT NULL            COMMENT '操作用户钱包',
  `role`           VARCHAR(16)  DEFAULT NULL            COMMENT '操作时的角色快照',
  `method`         VARCHAR(8)   NOT NULL                COMMENT 'HTTP 方法 (POST/PUT/DELETE)',
  `path`           VARCHAR(255) NOT NULL                COMMENT '请求路径',
  `operation`      VARCHAR(96)  NOT NULL                COMMENT 'Controller.method',
  `params`         TEXT                                 COMMENT '请求参数 (脱敏 + 截断 1500 字符)',
  `status`         VARCHAR(16)  NOT NULL                COMMENT 'SUCCESS / FAILURE',
  `error_message`  TEXT                                 COMMENT '失败异常信息',
  `request_ip`     VARCHAR(64)  DEFAULT NULL            COMMENT '请求来源 IP',
  `duration_ms`    BIGINT       DEFAULT NULL            COMMENT '处理耗时（毫秒）',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='操作审计日志';

-- ----------------------------------------------------------------------------
-- 测试数据：1 个管理员 + 2 个普通用户
-- 钱包地址使用 Hardhat 默认账号
-- ----------------------------------------------------------------------------
INSERT INTO `user` (`username`, `wallet_address`, `email`, `role`) VALUES
  ('admin', '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'admin@example.com', 'ADMIN'),
  ('alice', '0x70997970c51812dc3a010c7d01b50e0d17dc79c8', 'alice@example.com', 'USER'),
  ('bob',   '0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc', 'bob@example.com',   'USER');
