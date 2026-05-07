package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TraceExceptionWorkflowSchemaContractTest {

    @Test
    void initSchema_shouldIncludeExceptionRestoreSnapshotFieldsAndActionComments() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains(
                "exception_restore_status VARCHAR(32) NULL",
                "exception_restore_node VARCHAR(64) NULL",
                "exception_restore_owner VARCHAR(64) NULL",
                "EXCEPTION/EXCEPTION_OPEN/EXCEPTION_CLOSE/CORRECTION"
        );
    }

    @Test
    void migrationV20_shouldAddExceptionWorkflowFieldsAndDocumentActionTypes() throws IOException {
        String migration = readSql("migrate_v20_trace_exception_workflow.sql");

        assertThat(migration).contains(
                "ALTER TABLE trace_snapshot",
                "ADD COLUMN exception_restore_status",
                "ADD COLUMN exception_restore_node",
                "ADD COLUMN exception_restore_owner",
                "ALTER TABLE trace_lifecycle_log",
                "EXCEPTION_OPEN/EXCEPTION_CLOSE"
        );
    }

    private String readSql(String fileName) throws IOException {
        return Files.readString(Path.of("sql", fileName));
    }
}
