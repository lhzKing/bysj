package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceAggregation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DemoAggregationFactoryTest {

    private final DemoAggregationFactory factory = new DemoAggregationFactory();
    private final DemoUserRef creator = new DemoUserRef(101L, "producer");

    @Test
    void build_producesCartonsAndPalletsWithExpectedShape() {
        List<String> pool = pool(360); // enough for 24 cartons × 15 children
        List<TraceAggregation> rows = factory.build(
                pool, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        long cartonRows = rows.stream().filter(r -> "CARTON".equals(r.getRelationType())).count();
        long palletRows = rows.stream().filter(r -> "PALLET".equals(r.getRelationType())).count();

        // CARTON: 24 groups × [10, 15] children → between 240 and 360 carton rows
        assertThat(cartonRows).isBetween(240L, (long) DemoAggregationFactory.TARGET_CARTONS * DemoAggregationFactory.CARTON_SIZE_MAX);
        // PALLET: 6 pallets × 3 cartons = 18 rows
        assertThat(palletRows).isEqualTo((long) DemoAggregationFactory.TARGET_PALLETS * DemoAggregationFactory.CARTONS_PER_PALLET);
    }

    @Test
    void build_eachActiveChildIsBoundOnlyOnce() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(7L)
        );

        Set<String> seenCartonChildren = new HashSet<>();
        Set<String> seenPalletChildren = new HashSet<>();
        for (TraceAggregation row : rows) {
            if ("CARTON".equals(row.getRelationType())) {
                assertThat(seenCartonChildren.add(row.getChildCode()))
                        .as("trace_code %s bound to two cartons", row.getChildCode())
                        .isTrue();
            } else if ("PALLET".equals(row.getRelationType())) {
                assertThat(seenPalletChildren.add(row.getChildCode()))
                        .as("carton %s bound to two pallets", row.getChildCode())
                        .isTrue();
            }
        }
    }

    @Test
    void build_palletChildrenAreCartonParents() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(20260527L)
        );

        Set<String> cartonParents = new HashSet<>();
        for (TraceAggregation row : rows) {
            if ("CARTON".equals(row.getRelationType())) {
                cartonParents.add(row.getParentCode());
            }
        }
        for (TraceAggregation row : rows) {
            if ("PALLET".equals(row.getRelationType())) {
                assertThat(cartonParents)
                        .as("pallet child %s must be one of the carton parents we produced", row.getChildCode())
                        .contains(row.getChildCode());
            }
        }
    }

    @Test
    void build_setsActiveTrueAndPopulatesCreator() {
        List<String> pool = pool(360);
        List<TraceAggregation> rows = factory.build(
                pool, creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(42L)
        );

        for (TraceAggregation row : rows) {
            assertThat(row.getActive()).isTrue();
            assertThat(row.getCreateBy()).isEqualTo(101L);
            assertThat(row.getCreateByUsername()).isEqualTo("producer");
            assertThat(row.getReleaseTime()).isNull();
            assertThat(row.getBindTime()).isNotNull();
            assertThat(row.getParentCode()).isNotEqualTo(row.getChildCode());
        }
    }

    @Test
    void build_emptyPoolReturnsEmpty() {
        List<TraceAggregation> rows = factory.build(
                List.of(), creator,
                LocalDateTime.of(2026, 4, 1, 9, 0, 0),
                new Random(1L)
        );
        assertThat(rows).isEmpty();
    }

    private static List<String> pool(int n) {
        List<String> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add("TC-CAND-%04d".formatted(i));
        }
        return list;
    }
}
