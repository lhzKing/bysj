-- =====================================================================
-- 工业零配件供应链溯源系统 · 完整整合数据库脚本
-- =====================================================================
-- 生成日期：2026-05-23
-- 数据库版本：本脚本基于本机 `trace_db` 当前实际结构（mysqldump 等价）
--             整合自 init_schema.sql + migrate_v2 ~ v21 共 22 份增量脚本。
--
-- 包含内容：
--   1. 数据库与字符集
--   2. 全部 15 张表的最终结构（含索引、外键、CHECK 约束、生成列）
--   3. 基础种子数据：6 个角色 + 30 个权限 + 角色权限映射 + superadmin
--
-- 默认账号：superadmin / superadmin123456   （BCrypt 哈希已写入）
--
-- 使用方式：
--   mysql -u root -p < schema_consolidated.sql
--   或在 MySQL 8.0+ 中先 `CREATE DATABASE trace_db ...` 后逐段执行。
--
-- 注意：
--   * 本脚本只创建结构与基础种子，**不**写入演示业务数据。
--   * 如需演示数据，登录后调用：
--       POST /api/admin/generate-sample-data?count=500
--     或参考 backend/sql/sample_data_full.sql / reseed_demo_v2.sql。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 数据库
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `trace_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `trace_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. RBAC：角色 / 权限 / 用户
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;

CREATE TABLE `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_code`   VARCHAR(32)  NOT NULL COMMENT '角色代码',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色表';

CREATE TABLE `sys_permission` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `perm_code`   VARCHAR(64)  NOT NULL COMMENT '权限代码',
    `perm_name`   VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `api_method`  VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE/*',
    `api_pattern` VARCHAR(128) DEFAULT NULL COMMENT 'API 路径模式，支持 * 通配',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `perm_code` (`perm_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='权限表';

CREATE TABLE `sys_role_permission` (
    `role_id`       BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    KEY `fk_rp_perm` (`permission_id`),
    CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='角色权限关联表';

CREATE TABLE `sys_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(64)  NOT NULL,
    `password`      VARCHAR(128) NOT NULL,
    `role_id`       BIGINT       NOT NULL COMMENT '角色ID',
    `token_version` INT          NOT NULL DEFAULT 0 COMMENT 'Token版本号，改密/角色变更时递增',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`),
    KEY `fk_user_role` (`role_id`),
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户表';

-- ---------------------------------------------------------------------
-- 2. 配件主数据（SPU）
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `base_part_spec`;
CREATE TABLE `base_part_spec` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'SPU主键',
    `part_code`    VARCHAR(64)  NOT NULL COMMENT '规格编码',
    `part_name`    VARCHAR(128) NOT NULL COMMENT '配件名称',
    `part_type`    VARCHAR(64)  NOT NULL COMMENT '配件类别',
    `model`        VARCHAR(64)  DEFAULT NULL COMMENT '型号',
    `manufacturer` VARCHAR(128) DEFAULT NULL COMMENT '制造商',
    `unit`         VARCHAR(32)  DEFAULT NULL COMMENT '计量单位',
    `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用标志：1=启用 0=停用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `part_code` (`part_code`),
    KEY `idx_base_part_spec_enabled` (`enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='配件规格表（SPU）';

-- ---------------------------------------------------------------------
-- 3. 溯源业务节点 / 用户节点绑定
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `trace_user_node_binding`;
DROP TABLE IF EXISTS `trace_node`;

CREATE TABLE `trace_node` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '节点ID',
    `node_code`   VARCHAR(64)  NOT NULL COMMENT '节点编码',
    `node_name`   VARCHAR(64)  NOT NULL COMMENT '节点名称',
    `node_type`   VARCHAR(32)  NOT NULL COMMENT 'FACTORY/WAREHOUSE/LOGISTICS/CUSTOMER/SERVICE',
    `org_id`      BIGINT       DEFAULT NULL COMMENT '组织ID（B15 保留字段）',
    `province`    VARCHAR(32)  NOT NULL COMMENT '省',
    `city`        VARCHAR(32)  NOT NULL COMMENT '市',
    `address`     VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
    `enabled`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_node_code` (`node_code`),
    KEY `idx_trace_node_type` (`node_type`),
    KEY `idx_trace_node_enabled` (`enabled`),
    KEY `idx_trace_node_org_id` (`org_id`),
    KEY `idx_trace_node_region` (`province`, `city`),
    CONSTRAINT `ck_trace_node_type` CHECK (`node_type` IN ('FACTORY','WAREHOUSE','LOGISTICS','CUSTOMER','SERVICE')),
    CONSTRAINT `ck_trace_node_enabled` CHECK (`enabled` IN (0,1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='结构化溯源业务节点';

CREATE TABLE `trace_user_node_binding` (
    `id`           BIGINT     NOT NULL AUTO_INCREMENT COMMENT '用户-节点绑定ID',
    `user_id`      BIGINT     NOT NULL COMMENT '绑定的用户ID',
    `node_id`      BIGINT     NOT NULL COMMENT '绑定的节点ID',
    `org_id`       BIGINT     DEFAULT NULL COMMENT '组织ID（从 trace_node.org_id 拷贝）',
    `default_node` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 默认操作节点',
    `enabled`      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
    `create_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_user_node_binding` (`user_id`, `node_id`),
    KEY `idx_trace_user_node_user_enabled` (`user_id`, `enabled`),
    KEY `idx_trace_user_node_node_id` (`node_id`),
    KEY `idx_trace_user_node_org_id` (`org_id`),
    CONSTRAINT `fk_trace_user_node_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_trace_user_node_node` FOREIGN KEY (`node_id`) REFERENCES `trace_node` (`id`) ON DELETE CASCADE,
    CONSTRAINT `ck_trace_user_node_default` CHECK (`default_node` IN (0,1)),
    CONSTRAINT `ck_trace_user_node_enabled` CHECK (`enabled` IN (0,1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户可操作的溯源节点绑定';

-- ---------------------------------------------------------------------
-- 4. 赋码批次 / 单品码
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `trace_code`;
DROP TABLE IF EXISTS `trace_assign_batch`;

CREATE TABLE `trace_assign_batch` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '赋码批次ID',
    `batch_no`             VARCHAR(64)  NOT NULL COMMENT '赋码批次号',
    `production_order_no`  VARCHAR(64)  DEFAULT NULL COMMENT '生产计划/工单号',
    `spu_id`               BIGINT       NOT NULL COMMENT '关联SPU',
    `quantity_requested`   INT          NOT NULL COMMENT '请求生成数量',
    `quantity_generated`   INT          NOT NULL DEFAULT 0 COMMENT '已生成数量',
    `quantity_printed`     INT          NOT NULL DEFAULT 0 COMMENT '已打印数量',
    `quantity_activated`   INT          NOT NULL DEFAULT 0 COMMENT '已激活数量',
    `manufacturer_node_id` BIGINT       DEFAULT NULL COMMENT '生产节点ID',
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'CREATED'
        COMMENT 'CREATED/GENERATING/GENERATED/PARTIAL_FAILED/FAILED/CANCELLED',
    `operator_id`          BIGINT       DEFAULT NULL COMMENT '创建操作人ID',
    `operator_username`    VARCHAR(64)  DEFAULT NULL COMMENT '创建操作人用户名',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_assign_batch_no` (`batch_no`),
    KEY `idx_trace_assign_batch_spu_id` (`spu_id`),
    KEY `idx_trace_assign_batch_order_no` (`production_order_no`),
    KEY `idx_trace_assign_batch_status` (`status`),
    KEY `idx_trace_assign_batch_operator_id` (`operator_id`),
    CONSTRAINT `fk_trace_assign_batch_spu` FOREIGN KEY (`spu_id`) REFERENCES `base_part_spec` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_trace_assign_batch_operator` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_trace_assign_batch_quantity_requested` CHECK (`quantity_requested` > 0),
    CONSTRAINT `ck_trace_assign_batch_quantity_generated` CHECK (`quantity_generated` >= 0 AND `quantity_generated` <= `quantity_requested`),
    CONSTRAINT `ck_trace_assign_batch_quantity_printed`   CHECK (`quantity_printed` >= 0 AND `quantity_printed` <= `quantity_requested`),
    CONSTRAINT `ck_trace_assign_batch_quantity_activated` CHECK (`quantity_activated` >= 0 AND `quantity_activated` <= `quantity_requested`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='赋码批次表';

CREATE TABLE `trace_code` (
    `trace_code`            VARCHAR(64)  NOT NULL COMMENT '单品溯源码',
    `batch_id`              BIGINT       DEFAULT NULL COMMENT '所属赋码批次ID；兼容历史单码赋码允许为空',
    `spu_id`                BIGINT       NOT NULL COMMENT '关联SPU',
    `serial_no`             INT          DEFAULT NULL COMMENT '批次内序号',
    `qr_payload`            VARCHAR(512) NOT NULL COMMENT '二维码载荷',
    `code_status`           VARCHAR(32)  NOT NULL DEFAULT 'GENERATED'
        COMMENT 'GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED',
    `print_count`           INT          NOT NULL DEFAULT 0 COMMENT '打印/重打次数',
    `activated_time`        DATETIME     DEFAULT NULL COMMENT '激活时间',
    `activated_by`          BIGINT       DEFAULT NULL COMMENT '激活操作人ID',
    `activated_by_username` VARCHAR(64)  DEFAULT NULL COMMENT '激活操作人用户名',
    `current_snapshot_id`   VARCHAR(64)  DEFAULT NULL COMMENT '当前快照指针；当前等于 trace_snapshot.trace_code',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`trace_code`),
    UNIQUE KEY `uk_trace_code_batch_serial` (`batch_id`, `serial_no`),
    KEY `idx_trace_code_batch_id` (`batch_id`),
    KEY `idx_trace_code_spu_id` (`spu_id`),
    KEY `idx_trace_code_status` (`code_status`),
    KEY `idx_trace_code_activated_by` (`activated_by`),
    CONSTRAINT `fk_trace_code_batch` FOREIGN KEY (`batch_id`) REFERENCES `trace_assign_batch` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_trace_code_spu` FOREIGN KEY (`spu_id`) REFERENCES `base_part_spec` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_trace_code_activated_by` FOREIGN KEY (`activated_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_trace_code_print_count` CHECK (`print_count` >= 0),
    CONSTRAINT `ck_trace_code_serial_no` CHECK (`serial_no` IS NULL OR `serial_no` > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='单品码状态表';

-- ---------------------------------------------------------------------
-- 5. 溯源生命周期日志 + 当前状态快照 + 扫码幂等
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `trace_scan_idempotency`;
DROP TABLE IF EXISTS `trace_snapshot`;
DROP TABLE IF EXISTS `trace_lifecycle_log`;

CREATE TABLE `trace_lifecycle_log` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `trace_code`            VARCHAR(64)  NOT NULL COMMENT '溯源码',
    `spu_id`                BIGINT       NOT NULL COMMENT '关联SPU',
    `action_type`           VARCHAR(32)  NOT NULL
        COMMENT 'INIT/PRINT_CODE/REPRINT_CODE/ACTIVATE_CODE/VOID_CODE/PACK/UNPACK/PALLETIZE/UNPALLETIZE/INBOUND/OUTBOUND/TRANSFER/EXCEPTION/CORRECTION',
    `from_node`             VARCHAR(64)  DEFAULT NULL COMMENT '上游节点',
    `to_node`               VARCHAR(64)  DEFAULT NULL COMMENT '下游节点',
    `province`              VARCHAR(32)  DEFAULT NULL COMMENT '省（用于地图）',
    `city`                  VARCHAR(32)  DEFAULT NULL COMMENT '市（备用）',
    `remark`                VARCHAR(255) DEFAULT NULL COMMENT '事件备注（纳入Hash与签名计算）',
    `event_time`            DATETIME     NOT NULL COMMENT '业务发生时间(Event Time)',
    `ingest_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间(Ingest Time)',
    `prev_hash`             CHAR(64)     NOT NULL COMMENT '上一条日志Hash',
    `current_hash`          CHAR(64)     NOT NULL COMMENT '本条日志Hash',
    `correction_of`         BIGINT       DEFAULT NULL COMMENT '被修正的原日志ID',
    `operator`              VARCHAR(64)  NOT NULL COMMENT '操作人',
    `signature`             VARCHAR(512) DEFAULT NULL COMMENT 'RSA 数字签名',
    `signature_key_id`      VARCHAR(64)  NOT NULL DEFAULT 'default' COMMENT '签名密钥标识',
    `signature_key_version` INT          NOT NULL DEFAULT 1 COMMENT '签名密钥版本',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_trace_code` (`trace_code`),
    KEY `idx_event_time` (`event_time`),
    KEY `idx_ingest_time` (`ingest_time`),
    KEY `idx_province` (`province`),
    KEY `idx_from_to` (`from_node`, `to_node`),
    KEY `idx_correction_of` (`correction_of`),
    KEY `idx_signature_key` (`signature_key_id`, `signature_key_version`),
    CONSTRAINT `fk_correction_of` FOREIGN KEY (`correction_of`) REFERENCES `trace_lifecycle_log` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='溯源生命周期日志表（事实表）';

CREATE TABLE `trace_snapshot` (
    `trace_code`                VARCHAR(64) NOT NULL COMMENT '溯源码（主键）',
    `spu_id`                    BIGINT      NOT NULL COMMENT '关联SPU',
    `current_status`            VARCHAR(32) NOT NULL COMMENT '当前状态',
    `current_node`              VARCHAR(64) DEFAULT NULL COMMENT '当前节点',
    `current_owner`             VARCHAR(64) DEFAULT NULL COMMENT '当前责任方',
    `exception_restore_status`  VARCHAR(32) DEFAULT NULL COMMENT '异常关闭时还原的状态',
    `exception_restore_node`    VARCHAR(64) DEFAULT NULL COMMENT '异常关闭时还原的节点',
    `exception_restore_owner`   VARCHAR(64) DEFAULT NULL COMMENT '异常关闭时还原的责任方',
    `province`                  VARCHAR(32) DEFAULT NULL COMMENT '省（用于地图）',
    `city`                      VARCHAR(32) DEFAULT NULL COMMENT '市（备用）',
    `last_event_time`           DATETIME    NOT NULL COMMENT '最新业务时间',
    `last_log_id`               BIGINT      NOT NULL COMMENT '最新日志ID',
    `last_hash`                 CHAR(64)    NOT NULL COMMENT '链尾Hash（下一次prev_hash）',
    `update_time`               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `version`                   INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（@Version）',
    PRIMARY KEY (`trace_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='配件状态快照表';

CREATE TABLE `trace_scan_idempotency` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '幂等记录ID',
    `trace_code`       VARCHAR(64) NOT NULL COMMENT '溯源码',
    `action_type`      VARCHAR(32) NOT NULL COMMENT '扫码动作类型',
    `idempotency_key`  VARCHAR(64) NOT NULL COMMENT '客户端幂等键',
    `lifecycle_log_id` BIGINT      DEFAULT NULL COMMENT '首次成功产生的日志ID',
    `status`           VARCHAR(32) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCEEDED',
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_scan_idempotency` (`trace_code`, `action_type`, `idempotency_key`),
    KEY `idx_trace_scan_idempotency_log_id` (`lifecycle_log_id`),
    CONSTRAINT `fk_trace_scan_idempotency_log` FOREIGN KEY (`lifecycle_log_id`) REFERENCES `trace_lifecycle_log` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='扫码幂等记录表';

-- ---------------------------------------------------------------------
-- 6. 流转任务（运单）+ 任务扫码明细
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `trace_flow_task_scan`;
DROP TABLE IF EXISTS `trace_flow_task`;

CREATE TABLE `trace_flow_task` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流转任务ID',
    `task_no`              VARCHAR(64)  NOT NULL COMMENT '任务编号',
    `task_type`            VARCHAR(32)  NOT NULL COMMENT 'OUTBOUND/TRANSFER/INBOUND/RECEIVE',
    `source_node_id`       BIGINT       NOT NULL COMMENT '源节点ID',
    `target_node_id`       BIGINT       NOT NULL COMMENT '目标节点ID',
    `expected_quantity`    INT          NOT NULL COMMENT '应扫数量',
    `actual_quantity`      INT          NOT NULL DEFAULT 0 COMMENT '实扫数量',
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'CREATED'
        COMMENT 'CREATED/PROCESSING/COMPLETED/CANCELLED/EXCEPTION',
    `create_by`            BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_by_username`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人用户名',
    `complete_time`        DATETIME     DEFAULT NULL COMMENT '完成时间',
    `cancel_time`          DATETIME     DEFAULT NULL COMMENT '取消时间',
    `discrepancy_type`     VARCHAR(32)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/SHORTAGE/OVERAGE',
    `discrepancy_quantity` INT          NOT NULL DEFAULT 0 COMMENT '差异绝对值',
    `discrepancy_reason`   VARCHAR(255) DEFAULT NULL COMMENT '差异原因（应扫≠实扫时必填）',
    `discrepancy_time`     DATETIME     DEFAULT NULL COMMENT '差异记录时间',
    `remark`               VARCHAR(255) DEFAULT NULL COMMENT '业务备注',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_flow_task_no` (`task_no`),
    KEY `idx_trace_flow_task_type_status` (`task_type`, `status`),
    KEY `idx_trace_flow_task_source_node` (`source_node_id`),
    KEY `idx_trace_flow_task_target_node` (`target_node_id`),
    KEY `idx_trace_flow_task_create_by` (`create_by`),
    KEY `idx_trace_flow_task_create_time` (`create_time`),
    CONSTRAINT `fk_trace_flow_task_source_node` FOREIGN KEY (`source_node_id`) REFERENCES `trace_node` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_trace_flow_task_target_node` FOREIGN KEY (`target_node_id`) REFERENCES `trace_node` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_trace_flow_task_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_trace_flow_task_type` CHECK (`task_type` IN ('OUTBOUND','TRANSFER','INBOUND','RECEIVE')),
    CONSTRAINT `ck_trace_flow_task_status` CHECK (`status` IN ('CREATED','PROCESSING','COMPLETED','CANCELLED','EXCEPTION')),
    CONSTRAINT `ck_trace_flow_task_distinct_nodes` CHECK (`source_node_id` <> `target_node_id`),
    CONSTRAINT `ck_trace_flow_task_expected_quantity` CHECK (`expected_quantity` > 0),
    CONSTRAINT `ck_trace_flow_task_actual_quantity` CHECK (`actual_quantity` >= 0),
    CONSTRAINT `ck_trace_flow_task_discrepancy_type` CHECK (`discrepancy_type` IN ('NONE','SHORTAGE','OVERAGE')),
    CONSTRAINT `ck_trace_flow_task_discrepancy_quantity` CHECK (`discrepancy_quantity` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流转任务/运单表';

CREATE TABLE `trace_flow_task_scan` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '任务扫码明细ID',
    `task_id`           BIGINT      NOT NULL COMMENT '所属任务ID',
    `trace_code`        VARCHAR(64) NOT NULL COMMENT '扫描的单品码',
    `action_type`       VARCHAR(32) NOT NULL COMMENT 'OUTBOUND/INBOUND/TRANSFER',
    `counted`           TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '1 计入实扫数量',
    `operator_user_id`  BIGINT      DEFAULT NULL COMMENT '扫码人ID',
    `operator_username` VARCHAR(64) DEFAULT NULL COMMENT '扫码人用户名',
    `idempotency_key`   VARCHAR(64) DEFAULT NULL COMMENT '任务扫码幂等键',
    `scan_time`         DATETIME    NOT NULL COMMENT '扫码时间',
    `duplicate_count`   INT         NOT NULL DEFAULT 0 COMMENT '首次成功后的重复次数',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_flow_task_scan_code_action` (`task_id`, `trace_code`, `action_type`),
    KEY `idx_trace_flow_task_scan_task_time` (`task_id`, `scan_time`),
    KEY `idx_trace_flow_task_scan_trace_code` (`trace_code`),
    KEY `idx_trace_flow_task_scan_operator` (`operator_user_id`),
    CONSTRAINT `fk_trace_flow_task_scan_task` FOREIGN KEY (`task_id`) REFERENCES `trace_flow_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_trace_flow_task_scan_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_trace_flow_task_scan_action` CHECK (`action_type` IN ('OUTBOUND','INBOUND','TRANSFER')),
    CONSTRAINT `ck_trace_flow_task_scan_counted` CHECK (`counted` IN (0,1)),
    CONSTRAINT `ck_trace_flow_task_scan_duplicate_count` CHECK (`duplicate_count` >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='流转任务扫码明细（支持连续扫码）';

-- ---------------------------------------------------------------------
-- 7. 装箱 / 码垛 聚合关系
-- ---------------------------------------------------------------------

DROP TABLE IF EXISTS `trace_aggregation`;
CREATE TABLE `trace_aggregation` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '聚合关系ID',
    `parent_code`        VARCHAR(64)  NOT NULL COMMENT '父码（箱码/托码）',
    `child_code`         VARCHAR(64)  NOT NULL COMMENT '子码（子聚合码或单品码）',
    `relation_type`      VARCHAR(32)  NOT NULL COMMENT 'CARTON/PALLET/BATCH',
    `active`             TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 生效 0 已解绑',
    `active_marker`      TINYINT GENERATED ALWAYS AS (CASE WHEN `active` = 1 THEN 1 ELSE NULL END) STORED,
    `create_by`          BIGINT       DEFAULT NULL COMMENT '绑定操作人ID',
    `create_by_username` VARCHAR(64)  DEFAULT NULL COMMENT '绑定操作人用户名',
    `bind_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `release_time`       DATETIME     DEFAULT NULL COMMENT '解绑时间',
    `remark`             VARCHAR(255) DEFAULT NULL COMMENT '业务备注',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trace_aggregation_active_pair` (`parent_code`, `child_code`, `active_marker`),
    KEY `idx_trace_aggregation_parent_active` (`parent_code`, `active`),
    KEY `idx_trace_aggregation_child_active`  (`child_code`,  `active`),
    KEY `idx_trace_aggregation_type_active`   (`relation_type`, `active`),
    KEY `idx_trace_aggregation_create_by`     (`create_by`),
    CONSTRAINT `fk_trace_aggregation_create_by` FOREIGN KEY (`create_by`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `ck_trace_aggregation_active` CHECK (`active` IN (0,1)),
    CONSTRAINT `ck_trace_aggregation_distinct_codes` CHECK (`parent_code` <> `child_code`),
    CONSTRAINT `ck_trace_aggregation_relation_type` CHECK (`relation_type` IN ('CARTON','PALLET','BATCH'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT ='装箱/码垛聚合关系';

-- =====================================================================
-- 8. 基础种子数据
--    （从本机当前生产实例 dump，可作为干净环境的初始化基线）
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `sys_role_permission`;
TRUNCATE TABLE `sys_permission`;
DELETE FROM `sys_user`;
DELETE FROM `sys_role`;
ALTER TABLE `sys_role` AUTO_INCREMENT = 1;
ALTER TABLE `sys_permission` AUTO_INCREMENT = 1;
ALTER TABLE `sys_user` AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

-- 8.1 角色（6 个）
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `remark`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '拥有最高权限，可管理所有用户包括其他管理员'),
(2, 'ADMIN',       '系统管理员', '拥有管理权限，可管理普通用户'),
(3, 'PRODUCER',    '生产人员',   '可进行生产赋码'),
(4, 'WAREHOUSE',   '仓库人员',   '可进行入库/出库操作'),
(5, 'LOGISTICS',   '物流人员',   '可进行流转操作'),
(6, 'USER',        '普通用户',   '仅可查询溯源信息');

-- 8.2 权限（id 22-28 为历史空洞，刻意保留以与现存数据一致）
INSERT INTO `sys_permission` (`id`, `perm_code`, `perm_name`, `api_method`, `api_pattern`, `remark`) VALUES
(1,  'trace:create',         '生产赋码',          'POST',   '/api/traces',                      '创建溯源实例'),
(2,  'trace:scan',           '扫码流转',          'POST',   '/api/traces/*/events',             '记录流转事件'),
(3,  'trace:view',           '溯源查询',          'GET',    '/api/traces/*',                    '查看溯源详情'),
(4,  'dashboard:view',       '看板查看',          'GET',    '/api/dashboard/*',                 '查看统计数据'),
(5,  'user:view',            '查看用户',          'GET',    '/api/users/*',                     '查看用户列表和详情'),
(6,  'user:manage',          '管理用户',          '*',      '/api/users/*',                     '创建、修改、删除用户'),
(7,  'role:view',            '查看角色',          'GET',    '/api/roles/*',                     '查看角色和权限列表'),
(8,  'role:manage',          '管理角色',          '*',      '/api/roles/*',                     '增删改角色和权限分配'),
(9,  'part:view',            '查看配件',          'GET',    '/api/parts/*',                     '查看配件列表和详情'),
(10, 'part:manage',          '管理配件',          '*',      '/api/parts/*',                     '创建、修改、删除配件'),
(11, 'trace:inbound',        '入库操作',          'POST',   '/api/traces/*/events',             '允许执行入库扫码操作'),
(12, 'trace:outbound',       '出库操作',          'POST',   '/api/traces/*/events',             '允许执行出库扫码操作'),
(13, 'trace:transfer',       '物流流转',          'POST',   '/api/traces/*/events',             '允许执行物流流转扫码操作'),
(14, 'trace:audit:view',     '溯源审计视图',      NULL,     NULL,                                '查看溯源审计完整历史；由详情接口 view=audit 参数做业务级权限校验'),
(15, 'trace:batch:create',   '创建赋码批次',      'POST',   '/api/traces',                      '创建赋码批次并生成单品溯源码'),
(16, 'trace:code:print',     '打印/重打/作废标签', NULL,    NULL,                                '打印、重打或作废单品码标签；由控制器注解校验多个业务入口'),
(17, 'trace:code:activate',  '激活单品码',        'POST',   '/api/trace-codes/*/activate',      '贴码后扫码激活或复核单品码'),
(18, 'trace:task:create',    '创建流转任务',      'POST',   '/api/trace-flow-tasks',            '创建仓库/物流发货、入库或接收任务'),
(19, 'trace:task:scan',      '任务内扫码',        'POST',   '/api/trace-flow-tasks/*/scan',     '在流转任务内扫描单品码、箱码或托盘码'),
(20, 'trace:task:complete',  '完成流转任务',      'POST',   '/api/trace-flow-tasks/*/complete', '完成任务并处理少扫/多扫差异'),
(21, 'trace:exception:handle','处理溯源异常',     NULL,     NULL,                                '上报或处理溯源生命周期异常；由动作权限策略校验'),
(29, 'trace:data:generate',  '生成示例数据',      'POST',   '/api/admin/generate-sample-data',  '生成演示/验证用溯源数据'),
(30, 'trace:data:clear',     '清空溯源数据',      'DELETE', '/api/admin/clear-trace-data',      '危险操作：清空全部溯源日志和快照');

-- 8.3 角色—权限映射
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
-- SUPER_ADMIN 全量
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),(1,21),(1,29),(1,30),
-- ADMIN（去掉跨网点 inbound/outbound/transfer 三个低粒度权限）
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),
(2,14),(2,15),(2,16),(2,17),(2,18),(2,19),(2,20),(2,21),(2,29),(2,30),
-- PRODUCER：赋码 + 看板 + 配件 + 批次/打印/激活/异常
(3,1),(3,3),(3,4),(3,9),(3,15),(3,16),(3,17),(3,21),
-- WAREHOUSE：入/出库 + 任务 + 配件查看 + 异常
(4,3),(4,4),(4,9),(4,11),(4,12),(4,18),(4,19),(4,20),(4,21),
-- LOGISTICS：物流流转 + 任务 + 异常
(5,3),(5,4),(5,13),(5,18),(5,19),(5,20),(5,21),
-- USER：只读
(6,3),(6,4);

-- 8.4 默认超级管理员（密码：superadmin123456 / BCrypt cost=10）
INSERT INTO `sys_user` (`id`, `username`, `password`, `role_id`, `token_version`, `status`) VALUES
(1, 'superadmin', '$2a$10$3Z07pHScGSUSEWVGzSVlTeKc2GgI.GXw.Ui1moglCS..BWH.NN8e6', 1, 0, 1);

-- 重置 AUTO_INCREMENT 至下一个空位
ALTER TABLE `sys_role` AUTO_INCREMENT = 7;
ALTER TABLE `sys_permission` AUTO_INCREMENT = 31;
ALTER TABLE `sys_user` AUTO_INCREMENT = 2;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 完成。如需快速验证：
--   SELECT COUNT(*) FROM sys_role;             -- 6
--   SELECT COUNT(*) FROM sys_permission;       -- 23（去掉历史空洞）
--   SELECT COUNT(*) FROM sys_role_permission;  -- 69
--   SELECT username, role_id FROM sys_user;    -- superadmin / 1
-- =====================================================================
