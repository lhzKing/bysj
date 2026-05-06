package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TraceCodeSchemaContractTest {

    private static final Path INIT_SCHEMA = Paths.get("sql", "init_schema.sql");
    private static final Path MIGRATE_V11 = Paths.get("sql", "migrate_v11_trace_code_status.sql");

    @Test
    void initSchema_shouldCreateTraceCodeTableForFreshInstall() throws Exception {
        String src = Files.readString(INIT_SCHEMA);

        assertThat(src).contains("CREATE TABLE trace_code");
        assertThat(src).contains("trace_code VARCHAR(64) PRIMARY KEY");
        assertThat(src).contains("batch_id BIGINT NULL");
        assertThat(src).contains("spu_id BIGINT NOT NULL");
        assertThat(src).contains("serial_no INT NULL");
        assertThat(src).contains("qr_payload VARCHAR(512) NOT NULL");
        assertThat(src).contains("code_status VARCHAR(32) NOT NULL DEFAULT 'GENERATED'");
        assertThat(src).contains("print_count INT NOT NULL DEFAULT 0");
        assertThat(src).contains("activated_time DATETIME NULL");
        assertThat(src).contains("activated_by BIGINT NULL");
        assertThat(src).contains("current_snapshot_id VARCHAR(64) NULL");
        assertThat(src).contains("UNIQUE KEY uk_trace_code_batch_serial");
        assertThat(src).contains("CONSTRAINT fk_trace_code_batch");
        assertThat(src).contains("CONSTRAINT fk_trace_code_spu");
        assertThat(src).contains("CONSTRAINT ck_trace_code_print_count");
    }

    @Test
    void migrateV11_shouldCreateAndBackfillTraceCodeTableForExistingDeployments() throws Exception {
        String src = Files.readString(MIGRATE_V11);

        assertThat(src).contains("CREATE TABLE IF NOT EXISTS trace_code");
        assertThat(src).contains("GENERATED/PRINTED/ACTIVATED/IN_STOCK/IN_TRANSIT/EXCEPTION/VOIDED/SCRAPPED");
        assertThat(src).contains("INSERT INTO trace_code");
        assertThat(src).contains("FROM trace_snapshot s");
        assertThat(src).contains("LEFT JOIN trace_code c ON c.trace_code = s.trace_code");
        assertThat(src).contains("WHEN s.current_status = 'IN_STOCK' THEN 'IN_STOCK'");
        assertThat(src).contains("WHEN s.current_status IN ('IN_TRANSIT', 'TRANSFERRED') THEN 'IN_TRANSIT'");
        assertThat(src).contains("WHEN s.current_status = 'EXCEPTION' THEN 'EXCEPTION'");
        assertThat(src).contains("ELSE 'ACTIVATED'");
    }
}
