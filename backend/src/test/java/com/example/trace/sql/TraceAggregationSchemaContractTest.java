package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceAggregationSchemaContractTest {

    @Test
    void freshSchema_shouldIncludeAggregationRelationAfterFlowTaskScanAndBeforeTraceCode() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains(
                "CREATE TABLE trace_aggregation",
                "parent_code VARCHAR(64) NOT NULL",
                "child_code VARCHAR(64) NOT NULL",
                "relation_type VARCHAR(32) NOT NULL",
                "active TINYINT(1) NOT NULL DEFAULT 1",
                "active_marker TINYINT GENERATED ALWAYS AS (CASE WHEN active = 1 THEN 1 ELSE NULL END) STORED",
                "UNIQUE KEY uk_trace_aggregation_active_pair (parent_code, child_code, active_marker)",
                "INDEX idx_trace_aggregation_parent_active (parent_code, active)",
                "INDEX idx_trace_aggregation_child_active (child_code, active)",
                "CONSTRAINT fk_trace_aggregation_create_by FOREIGN KEY (create_by) REFERENCES sys_user(id) ON DELETE SET NULL",
                "CONSTRAINT ck_trace_aggregation_relation_type CHECK (relation_type IN ('CARTON','PALLET','BATCH'))",
                "CONSTRAINT ck_trace_aggregation_active CHECK (active IN (0, 1))",
                "CONSTRAINT ck_trace_aggregation_distinct_codes CHECK (parent_code <> child_code)"
        );
        assertThat(schema.indexOf("CREATE TABLE trace_flow_task_scan"))
                .isLessThan(schema.indexOf("CREATE TABLE trace_aggregation"));
        assertThat(schema.indexOf("CREATE TABLE trace_aggregation"))
                .isLessThan(schema.indexOf("CREATE TABLE trace_assign_batch"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_flow_task_scan"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_aggregation"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_aggregation"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_scan_idempotency"));
    }

    @Test
    void lifecycleLogSchema_shouldDocumentAggregationEventActions() throws IOException {
        String schema = readSql("init_schema.sql");
        String migration = readSql("migrate_v18_trace_aggregation_events.sql");

        assertThat(schema).contains("PACK/UNPACK/PALLETIZE/UNPALLETIZE");
        assertThat(migration).contains(
                "ALTER TABLE trace_lifecycle_log",
                "PACK/UNPACK/PALLETIZE/UNPALLETIZE"
        );
    }

    @Test
    void migrationV17_shouldCreateAggregationRelationTable() throws IOException {
        String migration = readSql("migrate_v17_trace_aggregation.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS trace_aggregation",
                "parent_code VARCHAR(64) NOT NULL",
                "child_code VARCHAR(64) NOT NULL",
                "relation_type VARCHAR(32) NOT NULL",
                "active TINYINT(1) NOT NULL DEFAULT 1",
                "active_marker TINYINT GENERATED ALWAYS AS (CASE WHEN active = 1 THEN 1 ELSE NULL END) STORED",
                "UNIQUE KEY uk_trace_aggregation_active_pair (parent_code, child_code, active_marker)",
                "CONSTRAINT ck_trace_aggregation_relation_type CHECK (relation_type IN ('CARTON','PALLET','BATCH'))",
                "CONSTRAINT ck_trace_aggregation_distinct_codes CHECK (parent_code <> child_code)"
        );
    }

    private String readSql(String fileName) throws IOException {
        return Files.readString(Path.of("sql", fileName));
    }
}
