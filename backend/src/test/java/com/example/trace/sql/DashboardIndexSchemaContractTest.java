package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks down the Dashboard performance indexes (T-P1-03).
 *
 * <p>The pair of indexes added in this task — {@code idx_trace_snapshot_last_event_time}
 * on {@code trace_snapshot(last_event_time)} and {@code idx_action_type} on
 * {@code trace_lifecycle_log(action_type)} — directly back the rewritten
 * {@code selectKpi} query (sargable today_new + EXCEPTION SUM). If either index
 * is silently dropped from the schema or the migration script, the dashboard
 * KPI endpoint regresses to full-table scans on production-scale data.</p>
 *
 * <p>This test enforces:
 * <ul>
 *   <li>Both indexes appear in {@code init_schema.sql} (fresh-install path).</li>
 *   <li>Both indexes appear in {@code migrate_v7_dashboard_indexes.sql}
 *       (existing-DB path), which is the only file under
 *       {@code backend/sql/migrate_v7_*.sql}.</li>
 * </ul>
 */
class DashboardIndexSchemaContractTest {

    private static final Path INIT_SCHEMA = Paths.get("sql", "init_schema.sql");
    private static final Path MIGRATE_V7 = Paths.get("sql", "migrate_v7_dashboard_indexes.sql");

    @Test
    void initSchemaShipsBothDashboardIndexes() throws IOException {
        String src = Files.readString(INIT_SCHEMA);

        // Snapshot today-new index.
        assertThat(src)
                .as("init_schema.sql must register idx_trace_snapshot_last_event_time")
                .contains("idx_trace_snapshot_last_event_time")
                .contains("(last_event_time)");

        // Lifecycle-log action_type index.
        assertThat(src)
                .as("init_schema.sql must register idx_action_type on trace_lifecycle_log")
                .contains("idx_action_type")
                // Comment guard: ensure the index annotation is present so future readers
                // understand why this single-column index exists despite low cardinality.
                .contains("Dashboard selectKpi `exception_count`");
    }

    @Test
    void migrateV7AddsBothDashboardIndexesIdempotently() throws IOException {
        String src = Files.readString(MIGRATE_V7);

        assertThat(src)
                .as("migrate_v7 must add idx_trace_snapshot_last_event_time")
                .contains("idx_trace_snapshot_last_event_time")
                .contains("CREATE INDEX idx_trace_snapshot_last_event_time ON trace_snapshot(last_event_time)");

        assertThat(src)
                .as("migrate_v7 must add idx_action_type on trace_lifecycle_log")
                .contains("CREATE INDEX idx_action_type ON trace_lifecycle_log(action_type)");

        // The migration must be idempotent (re-runnable) — guarded via INFORMATION_SCHEMA lookup.
        assertThat(src)
                .as("migrate_v7 must be idempotent — guard via information_schema.STATISTICS lookup")
                .contains("information_schema.STATISTICS")
                .contains("INDEX_NAME = 'idx_trace_snapshot_last_event_time'")
                .contains("INDEX_NAME = 'idx_action_type'");
    }
}
