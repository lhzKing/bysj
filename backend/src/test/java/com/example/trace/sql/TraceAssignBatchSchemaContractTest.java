package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TraceAssignBatchSchemaContractTest {

    private static final Path INIT_SCHEMA = Paths.get("sql", "init_schema.sql");
    private static final Path MIGRATE_V10 = Paths.get("sql", "migrate_v10_trace_assign_batch.sql");

    @Test
    void initSchema_shouldCreateTraceAssignBatchTableForFreshInstall() throws Exception {
        String src = Files.readString(INIT_SCHEMA);

        assertThat(src).contains("CREATE TABLE trace_assign_batch");
        assertThat(src).contains("batch_no VARCHAR(64) NOT NULL");
        assertThat(src).contains("production_order_no VARCHAR(64) NULL");
        assertThat(src).contains("spu_id BIGINT NOT NULL");
        assertThat(src).contains("quantity_requested INT NOT NULL");
        assertThat(src).contains("quantity_generated INT NOT NULL DEFAULT 0");
        assertThat(src).contains("quantity_printed INT NOT NULL DEFAULT 0");
        assertThat(src).contains("quantity_activated INT NOT NULL DEFAULT 0");
        assertThat(src).contains("manufacturer_node_id BIGINT NULL");
        assertThat(src).contains("status VARCHAR(32) NOT NULL DEFAULT 'CREATED'");
        assertThat(src).contains("UNIQUE KEY uk_trace_assign_batch_no");
        assertThat(src).contains("CONSTRAINT fk_trace_assign_batch_spu");
        assertThat(src).contains("CONSTRAINT fk_trace_assign_batch_operator");
    }

    @Test
    void migrateV10_shouldCreateTraceAssignBatchTableForExistingDeployments() throws Exception {
        String src = Files.readString(MIGRATE_V10);

        assertThat(src).contains("CREATE TABLE IF NOT EXISTS trace_assign_batch");
        assertThat(src).contains("UNIQUE KEY uk_trace_assign_batch_no");
        assertThat(src).contains("idx_trace_assign_batch_spu_id");
        assertThat(src).contains("ck_trace_assign_batch_quantity_requested");
        assertThat(src).contains("manufacturer_node_id BIGINT NULL");
    }
}
