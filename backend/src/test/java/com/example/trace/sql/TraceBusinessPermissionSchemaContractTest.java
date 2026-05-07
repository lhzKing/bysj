package com.example.trace.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TraceBusinessPermissionSchemaContractTest {

    @Test
    void initSchema_shouldSeedBusinessActionPermissionsAndRoleAssignments() throws IOException {
        String schema = readSql("init_schema.sql");

        assertThat(schema).contains(
                "'trace:batch:create'",
                "'trace:code:print'",
                "'trace:code:activate'",
                "'trace:task:create'",
                "'trace:task:scan'",
                "'trace:task:complete'",
                "'trace:exception:handle'"
        );
        assertThat(schema).contains("(3, 17)", "(3, 18)", "(3, 19)", "(3, 23)");
        assertThat(schema).contains("(4, 20)", "(4, 21)", "(4, 22)", "(4, 23)");
        assertThat(schema).contains("(5, 20)", "(5, 21)", "(5, 22)", "(5, 23)");
    }

    @Test
    void migrationV19_shouldInsertPermissionsAndGrantBuiltInOperationalRoles() throws IOException {
        String migration = readSql("migrate_v19_trace_business_action_permissions.sql");

        assertThat(migration).contains(
                "INSERT INTO sys_permission",
                "'trace:batch:create'",
                "'trace:code:print'",
                "'trace:code:activate'",
                "'trace:task:create'",
                "'trace:task:scan'",
                "'trace:task:complete'",
                "'trace:exception:handle'",
                "ON DUPLICATE KEY UPDATE"
        );
        assertThat(migration).contains("r.role_code IN ('SUPER_ADMIN', 'ADMIN')");
        assertThat(migration).contains("r.role_code = 'PRODUCER'");
        assertThat(migration).contains("r.role_code = 'WAREHOUSE'");
        assertThat(migration).contains("r.role_code = 'LOGISTICS'");
    }

    private static String readSql(String filename) throws IOException {
        return Files.readString(Paths.get("sql", filename));
    }
}
