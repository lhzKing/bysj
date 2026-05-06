package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceNodeSchemaContractTest {

    @Test
    void freshSchema_shouldDefineStructuredTraceNodeAndBatchManufacturerRelationship() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains("CREATE TABLE trace_node");
        assertThat(schema).contains(
                "node_code VARCHAR(64) NOT NULL",
                "node_name VARCHAR(64) NOT NULL",
                "node_type VARCHAR(32) NOT NULL",
                "org_id BIGINT NULL",
                "province VARCHAR(32) NOT NULL",
                "city VARCHAR(32) NOT NULL",
                "address VARCHAR(255) NULL",
                "enabled TINYINT(1) NOT NULL DEFAULT 1"
        );
        assertThat(schema).contains(
                "UNIQUE KEY uk_trace_node_code (node_code)",
                "INDEX idx_trace_node_type (node_type)",
                "INDEX idx_trace_node_enabled (enabled)",
                "INDEX idx_trace_node_org_id (org_id)",
                "INDEX idx_trace_node_region (province, city)",
                "CONSTRAINT ck_trace_node_type CHECK (node_type IN ('FACTORY','WAREHOUSE','LOGISTICS','CUSTOMER','SERVICE'))"
        );
        assertThat(schema).contains(
                "manufacturer_node_id BIGINT NULL",
                "INDEX idx_trace_assign_batch_manufacturer_node_id (manufacturer_node_id)",
                "CONSTRAINT fk_trace_assign_batch_manufacturer_node FOREIGN KEY (manufacturer_node_id) REFERENCES trace_node(id) ON DELETE SET NULL"
        );
        assertThat(schema.indexOf("CREATE TABLE trace_node")).isLessThan(schema.indexOf("CREATE TABLE trace_assign_batch"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_assign_batch"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_node"));
    }

    @Test
    void migrationV12_shouldCreateTraceNodeWithoutAddingUnsafeBatchForeignKey() throws IOException {
        String migration = readSql("migrate_v12_trace_node.sql");

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS trace_node");
        assertThat(migration).contains(
                "node_code VARCHAR(64) NOT NULL",
                "node_name VARCHAR(64) NOT NULL",
                "node_type VARCHAR(32) NOT NULL",
                "CONSTRAINT ck_trace_node_enabled CHECK (enabled IN (0, 1))",
                "CONSTRAINT ck_trace_node_type CHECK (node_type IN ('FACTORY','WAREHOUSE','LOGISTICS','CUSTOMER','SERVICE'))"
        );
        assertThat(migration).doesNotContain("ALTER TABLE trace_assign_batch");
        assertThat(migration).doesNotContain("fk_trace_assign_batch_manufacturer_node");
    }

    private static String readSql(String fileName) throws IOException {
        return Files.readString(Path.of("sql", fileName));
    }
}
