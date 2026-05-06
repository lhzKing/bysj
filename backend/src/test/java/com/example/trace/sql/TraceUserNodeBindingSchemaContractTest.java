package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceUserNodeBindingSchemaContractTest {

    @Test
    void freshSchema_shouldIncludeUserNodeBindingTableAfterTraceNode() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains(
                "CREATE TABLE trace_user_node_binding",
                "user_id BIGINT NOT NULL",
                "node_id BIGINT NOT NULL",
                "org_id BIGINT NULL",
                "default_node TINYINT(1) NOT NULL DEFAULT 0",
                "enabled TINYINT(1) NOT NULL DEFAULT 1",
                "UNIQUE KEY uk_trace_user_node_binding (user_id, node_id)",
                "INDEX idx_trace_user_node_user_enabled (user_id, enabled)",
                "INDEX idx_trace_user_node_node_id (node_id)",
                "CONSTRAINT fk_trace_user_node_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE",
                "CONSTRAINT fk_trace_user_node_node FOREIGN KEY (node_id) REFERENCES trace_node(id) ON DELETE CASCADE"
        );
        assertThat(schema.indexOf("CREATE TABLE trace_node"))
                .isLessThan(schema.indexOf("CREATE TABLE trace_user_node_binding"));
        assertThat(schema.indexOf("DROP TABLE IF EXISTS trace_user_node_binding"))
                .isLessThan(schema.indexOf("DROP TABLE IF EXISTS trace_node"));
    }

    @Test
    void migrationV13_shouldCreateUserNodeBindingTable() throws IOException {
        String migration = readSql("migrate_v13_trace_user_node_binding.sql");

        assertThat(migration).contains(
                "CREATE TABLE IF NOT EXISTS trace_user_node_binding",
                "UNIQUE KEY uk_trace_user_node_binding (user_id, node_id)",
                "CONSTRAINT ck_trace_user_node_default CHECK (default_node IN (0, 1))",
                "CONSTRAINT ck_trace_user_node_enabled CHECK (enabled IN (0, 1))"
        );
    }

    private String readSql(String fileName) throws IOException {
        return Files.readString(Path.of("sql", fileName));
    }
}
