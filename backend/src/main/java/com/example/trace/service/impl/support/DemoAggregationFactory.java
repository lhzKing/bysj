package com.example.trace.service.impl.support;

import com.example.trace.entity.TraceAggregation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Demo aggregation factory.
 *
 * <p>Builds:</p>
 * <ul>
 *   <li>24 carton-level groupings (parent_code = {@code CTN-EXT-…}) each with
 *       10–15 child trace codes, relation_type = {@code CARTON}</li>
 *   <li>6 pallet-level groupings (parent_code = {@code AGG-EXT-PALLET-…}) each
 *       grouping 3 cartons, relation_type = {@code PALLET}</li>
 * </ul>
 *
 * <p>Each row sets {@code active=1}, {@code bind_time} on a deterministic
 * timeline derived from {@code base_time}. {@code active_marker} is a STORED
 * GENERATED column in MySQL and is NOT written — it's omitted from the entity
 * so MyBatis-Plus doesn't try to assign it.</p>
 *
 * <p>Carton parents are not themselves trace_code rows (a carton is a
 * higher-level container code; only single items live in trace_code).</p>
 */
public class DemoAggregationFactory {

    public static final int TARGET_CARTONS = 24;
    public static final int TARGET_PALLETS = 6;
    public static final int CARTON_SIZE_MIN = 10;
    public static final int CARTON_SIZE_MAX = 15;
    public static final int CARTONS_PER_PALLET = 3;

    private static final String DEMO_REMARK = "由 generate-sample-data 接口批量生成";

    /**
     * @param aggregatableTraceCodes pool of trace codes eligible for packing
     *                               (status IN_STOCK / IN_TRANSIT)
     * @param createdBy              demo user that owns the binding action
     * @param baseTime               base timeline (cartons start at +15 days, pallets at +18 days)
     * @param rng                    shared RNG
     */
    public List<TraceAggregation> build(List<String> aggregatableTraceCodes,
                                        DemoUserRef createdBy,
                                        LocalDateTime baseTime, Random rng) {
        List<TraceAggregation> rows = new ArrayList<>();
        if (aggregatableTraceCodes.isEmpty()) {
            return rows;
        }
        List<String> pool = new ArrayList<>(aggregatableTraceCodes);
        Collections.shuffle(pool, rng);

        List<String> cartonCodes = new ArrayList<>(TARGET_CARTONS);
        int cursor = 0;
        for (int c = 0; c < TARGET_CARTONS && cursor < pool.size(); c++) {
            int size = CARTON_SIZE_MIN + rng.nextInt(CARTON_SIZE_MAX - CARTON_SIZE_MIN + 1);
            int end = Math.min(cursor + size, pool.size());
            if (end - cursor < CARTON_SIZE_MIN && !cartonCodes.isEmpty()) {
                // Don't produce a degenerate small carton; stop here.
                break;
            }
            String cartonCode = "CTN-EXT-%03d-%s".formatted(c + 1, shortUuid());
            LocalDateTime bindTime = baseTime.plusDays(15L).plusHours(c)
                    .withSecond(33).withNano(0).truncatedTo(ChronoUnit.SECONDS);
            for (int i = cursor; i < end; i++) {
                rows.add(buildRow(cartonCode, pool.get(i), "CARTON", createdBy, bindTime));
            }
            cartonCodes.add(cartonCode);
            cursor = end;
        }

        for (int p = 0; p < TARGET_PALLETS; p++) {
            int start = p * CARTONS_PER_PALLET;
            int end = start + CARTONS_PER_PALLET;
            if (end > cartonCodes.size()) {
                break;
            }
            String palletCode = "AGG-EXT-PALLET-%03d-%s".formatted(p + 1, shortUuid());
            LocalDateTime bindTime = baseTime.plusDays(18L).plusHours(p + 1)
                    .withSecond(44).withNano(0).truncatedTo(ChronoUnit.SECONDS);
            for (int i = start; i < end; i++) {
                rows.add(buildRow(palletCode, cartonCodes.get(i), "PALLET", createdBy, bindTime));
            }
        }
        return rows;
    }

    private static TraceAggregation buildRow(String parentCode, String childCode, String relationType,
                                             DemoUserRef createdBy, LocalDateTime bindTime) {
        TraceAggregation row = new TraceAggregation();
        row.setParentCode(parentCode);
        row.setChildCode(childCode);
        row.setRelationType(relationType);
        row.setActive(true);
        if (createdBy != null) {
            row.setCreateBy(createdBy.id());
            row.setCreateByUsername(createdBy.username());
        }
        row.setBindTime(bindTime);
        row.setReleaseTime(null);
        row.setRemark(DEMO_REMARK);
        return row;
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
