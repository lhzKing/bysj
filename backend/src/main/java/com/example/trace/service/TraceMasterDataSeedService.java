package com.example.trace.service;

import java.util.Map;

/**
 * Seeds the master-data tables required before any business demo data can be
 * generated. Calling this is the prerequisite step for
 * {@code POST /api/admin/generate-sample-data}.
 *
 * <p><b>Idempotent</b>: every row is upserted by business key (username / node_code
 * / part_code / (user_id, node_id)), so calling repeatedly returns the same DB
 * state with most counts as {@code skipped}.</p>
 */
public interface TraceMasterDataSeedService {

    /**
     * Seed demo users, trace nodes, SPU specifications, and user-node bindings.
     *
     * @param operator      acting username (for audit logs)
     * @param operatorRole  acting role code
     * @return per-table inserted/skipped counts
     */
    Map<String, Object> seedMasterData(String operator, String operatorRole);
}
