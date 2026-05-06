package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFlowTaskSchemaContractTest {

    @Test
    void freshSchema_shouldIncludeFlowTaskTableWithNodeAndUserReferences() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains(
                "CREATE TABLE trace_flow_task",
                "task_no VARCHAR(64) NOT NULL",
                "task_type VARCHAR(32) NOT NULL",
                "source_node_id BIGINT NOT NULL",
                "target_node_id BIGINT NOT NULL",
                "expected_quantity INT NOT NULL",
                "actual_quantity INT NOT NULL DEFAULT 0",
                "status VARCHAR(32) NOT NULL DEFAULT 'CREATED'",
                "discrepancy_type VARCHAR(32) NOT NULL DEFAULT 'NONE'",
                "discrepancy_quantity INT NOT NULL DEFAULT 0",
                "discrepancy_reason VARCHAR(255) NULL",
                "discrepancy_time DATETIME NULL",
                "UNIQUE KEY uk_trace_flow_task_no (task_no)",
                "INDEX idx_trace_flow_task_type_status (task_type, status)",
                "CONSTRAINT fk_trace_flow_task_source_node FOREIGN KEY (source_node_id) REFERENCES trace_node(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_trace_flow_task_target_node FOREIGN KEY (target_node_id) REFERENCES trace_node(id) ON DELETE RESTRICT",
                "CONSTRAINT fk_trace_flow_task_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id) ON DELETE SET NULL",
                "CONSTRAINT ck_trace_flow_task_type CHECK (task_type IN ('OUTBOUND','TRANSFER','INBOUND','RECEIVE'))",
                "CONSTRAINT ck_trace_flow_task_status CHECK (status IN ('CREATED','PROCESSING','COMPLETED','CANCELLED','EXCEPTION'))",
                "CONSTRAINT ck_trace_flow_task_discrepancy_type CHECK (discrepancy_type IN ('NONE','SHORTAGE','OVERAGE'))",
                "CONSTRAINT ck_trace_flow_task_discrepancy_quantity CHECK (discrepancy_quantity >= 0)"
        );
        assertThat(schema.indexOf("CREATE TABLE trace_node"))
                .isLessThan(schema.indexOf("CREATE TABLE trace_flow_task"));
        assertThat(schema).contains(
                "CREATE TABLE trace_flow_task_scan",
                "task_id BIGINT NOT NULL",
                "trace_code VARCHAR(64) NOT NULL",
                "action_type VARCHAR(32) NOT NULL",
                "counted TINYINT(1) NOT NULL DEFAULT 1",
                "duplicate_count INT NOT NULL DEFAULT 0",
                "UNIQUE KEY uk_trace_flow_task_scan_code_action (task_id, trace_code, action_type)",
                "CONSTRAINT fk_trace_flow_task_scan_task FOREIGN KEY (task_id) REFERENCES trace_flow_task(id) ON DELETE CASCADE",
                "CONSTRAINT fk_trace_flow_task_scan_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user(id) ON DELETE SET NULL",
                "CONSTRAINT ck_trace_flow_task_scan_action CHECK (action_type IN ('OUTBOUND','INBOUND','TRANSFER'))"
        );
        assertThat(schema.indexOf("CREATE TABLE trace_flow_task ("))
                .isLessThan(schema.indexOf("CREATE TABLE trace_flow_task_scan"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_flow_task_scan"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_flow_task;"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_flow_task;"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_node"));
    }

    @Test
    void migrationV14_shouldCreateFlowTaskTable() throws IOException {
        String migration = readSql("migrate_v14_trace_flow_task.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS trace_flow_task",
                "UNIQUE KEY uk_trace_flow_task_no (task_no)",
                "CONSTRAINT ck_trace_flow_task_distinct_nodes CHECK (source_node_id <> target_node_id)"
        );
    }

    @Test
    void migrationV15_shouldCreateFlowTaskScanDetailTable() throws IOException {
        String migration = readSql("migrate_v15_trace_flow_task_scan.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS trace_flow_task_scan",
                "UNIQUE KEY uk_trace_flow_task_scan_code_action (task_id, trace_code, action_type)",
                "CONSTRAINT fk_trace_flow_task_scan_task FOREIGN KEY (task_id) REFERENCES trace_flow_task(id) ON DELETE CASCADE",
                "CONSTRAINT ck_trace_flow_task_scan_duplicate_count CHECK (duplicate_count >= 0)"
        );
    }

    @Test
    void migrationV16_shouldAddTaskCompletionDiscrepancyFields() throws IOException {
        String migration = readSql("migrate_v16_trace_flow_task_discrepancy.sql");

        assertThat(migration).contains(
                "ADD COLUMN discrepancy_type VARCHAR(32) NOT NULL DEFAULT 'NONE'",
                "ADD COLUMN discrepancy_quantity INT NOT NULL DEFAULT 0",
                "ADD COLUMN discrepancy_reason VARCHAR(255) NULL",
                "ADD COLUMN discrepancy_time DATETIME NULL",
                "ADD CONSTRAINT ck_trace_flow_task_discrepancy_type",
                "ADD CONSTRAINT ck_trace_flow_task_discrepancy_quantity"
        );
    }

    private String readSql(String fileName) throws IOException {
        return Files.readString(Path.of("sql", fileName));
    }
}
