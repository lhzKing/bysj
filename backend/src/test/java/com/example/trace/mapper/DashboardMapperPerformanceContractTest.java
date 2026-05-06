package com.example.trace.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure file-content audit on {@code DashboardMapper.xml}.
 *
 * <p>Why this test exists (T-P1-03): the dashboard SQL has three guarantees that
 * are hard to express as integration tests in a thesis-scale codebase
 * (no embedded MySQL, no @SpringBootTest infra for raw SQL audits) but easy to
 * lock down at the file level. If a future edit accidentally drops a LIMIT,
 * regresses the sargable today_new rewrite, or removes the topology
 * frequency-sort, this audit fails immediately.</p>
 *
 * <ul>
 *   <li>{@code selectKpi.today_new} must NOT wrap last_event_time in
 *       {@code DATE(...)}; the previous form forced a full-table scan even on
 *       indexed snapshots.</li>
 *   <li>{@code selectMapData} must end with a hard LIMIT to cap the response
 *       payload at the province-count level (we set 50, well above China's 34
 *       province-level admin units).</li>
 *   <li>{@code selectTopologyEdges} must sort by edge frequency descending and
 *       hard-cap with LIMIT to prevent unbounded responses on large
 *       lifecycle-log tables.</li>
 * </ul>
 */
class DashboardMapperPerformanceContractTest {

    private static final Path DASHBOARD_MAPPER_XML = Paths.get(
            "src", "main", "resources", "mapper", "DashboardMapper.xml");

    private String mapperSource() throws IOException {
        return Files.readString(DASHBOARD_MAPPER_XML);
    }

    @Test
    void selectKpiTodayNewMustBeSargableInsteadOfWrappingDateOnLastEventTime() throws IOException {
        String src = mapperSource();
        // Negative assertion: the legacy DATE(last_event_time) = CURDATE() form must NOT return.
        assertThat(src)
                .as("selectKpi today_new must not wrap last_event_time in DATE(...) — that forces a full scan")
                .doesNotContain("DATE(last_event_time) = CURDATE()");

        // Positive assertion: the rewritten range filter must be present.
        assertThat(src)
                .as("selectKpi today_new must use a sargable range filter")
                .contains("last_event_time >= CURDATE()")
                .contains("DATE_ADD(CURDATE(), INTERVAL 1 DAY)");
    }

    @Test
    void selectMapDataMustHardCapResultsWithLimit50() throws IOException {
        String src = mapperSource();
        int mapStart = src.indexOf("<select id=\"selectMapData\"");
        int mapEnd = src.indexOf("</select>", mapStart);
        assertThat(mapStart).as("selectMapData must exist").isNotNegative();
        assertThat(mapEnd).as("selectMapData must close").isGreaterThan(mapStart);

        String body = src.substring(mapStart, mapEnd);
        assertThat(body)
                .as("selectMapData must end with LIMIT 50 to cap province-aggregated payload")
                .contains("LIMIT 50");
    }

    @Test
    void selectTopologyEdgesMustOrderByFrequencyAndHardCapWithLimit200() throws IOException {
        String src = mapperSource();
        int topoStart = src.indexOf("<select id=\"selectTopologyEdges\"");
        int topoEnd = src.indexOf("</select>", topoStart);
        assertThat(topoStart).as("selectTopologyEdges must exist").isNotNegative();
        assertThat(topoEnd).as("selectTopologyEdges must close").isGreaterThan(topoStart);

        String body = src.substring(topoStart, topoEnd);
        // Must expose a frequency column for the ORDER BY to be meaningful.
        assertThat(body)
                .as("selectTopologyEdges must compute COUNT(*) AS edge_weight")
                .contains("COUNT(*) AS edge_weight");
        // Must sort highest-frequency edges first so the LIMIT keeps the most
        // representative subset of the topology graph.
        assertThat(body)
                .as("selectTopologyEdges must ORDER BY edge_weight DESC")
                .contains("ORDER BY edge_weight DESC");
        // Hard cap.
        assertThat(body)
                .as("selectTopologyEdges must LIMIT 200")
                .contains("LIMIT 200");
    }
}
