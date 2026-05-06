package com.example.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for batched persistence of long-running write paths
 * (produce-assign + demo-data generation).
 *
 * <p>Why this exists (T-P1-01): RSA-2048 signing dominates the per-record cost
 * (~5-10 ms each). When batched inside a single transaction at quantity=500 the
 * DB connection is held for several seconds, which blocks the HikariCP pool
 * under any realistic concurrency. Splitting into stage-1 (in-memory build +
 * sign, no transaction) plus stage-2 (chunked {@code REQUIRES_NEW} commits)
 * keeps each transaction short. {@code commitSize} controls the chunk size for
 * stage-2.</p>
 */
@ConfigurationProperties(prefix = "trace.batch")
public class TraceBatchProperties {

    /** Hard floor — committing one record at a time defeats the purpose. */
    public static final int MIN_COMMIT_SIZE = 1;
    /** Hard ceiling — beyond this, single-batch transaction time grows again. */
    public static final int MAX_COMMIT_SIZE = 500;
    /** Default chunk size; tuned for ~50 INSERTs per transaction (~50-100 ms wall-clock). */
    public static final int DEFAULT_COMMIT_SIZE = 50;

    private int commitSize = DEFAULT_COMMIT_SIZE;

    public int getCommitSize() {
        return commitSize;
    }

    public void setCommitSize(int commitSize) {
        if (commitSize < MIN_COMMIT_SIZE || commitSize > MAX_COMMIT_SIZE) {
            throw new IllegalArgumentException("trace.batch.commit-size must be between "
                    + MIN_COMMIT_SIZE + " and " + MAX_COMMIT_SIZE);
        }
        this.commitSize = commitSize;
    }
}
