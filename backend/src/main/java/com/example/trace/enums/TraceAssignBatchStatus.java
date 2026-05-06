package com.example.trace.enums;

/**
 * Assignment-batch lifecycle status.
 *
 * <p>B08 only introduces the batch container. Later tasks (B10-B13) will wire
 * these states into real code generation, printing and activation workflows.</p>
 */
public enum TraceAssignBatchStatus {

    /** Batch has been created but no code generation has started. */
    CREATED,

    /** Code generation is currently running. */
    GENERATING,

    /** All requested single-item trace codes were generated. */
    GENERATED,

    /** Some, but not all, requested codes were generated. */
    PARTIAL_FAILED,

    /** No requested codes were generated successfully. */
    FAILED,

    /** Batch was cancelled before completion. */
    CANCELLED
}
