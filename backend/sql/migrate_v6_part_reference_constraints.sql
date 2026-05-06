-- R-P1-06: protect base_part_spec from deleting SPUs already used by trace data.
-- Run after confirming existing trace_snapshot / trace_lifecycle_log rows have valid spu_id values.

CREATE INDEX idx_trace_snapshot_spu_id ON trace_snapshot(spu_id);
CREATE INDEX idx_trace_lifecycle_log_spu_id ON trace_lifecycle_log(spu_id);

ALTER TABLE trace_snapshot
  ADD CONSTRAINT fk_trace_snapshot_spu
  FOREIGN KEY (spu_id) REFERENCES base_part_spec(id)
  ON DELETE RESTRICT;

ALTER TABLE trace_lifecycle_log
  ADD CONSTRAINT fk_trace_log_spu
  FOREIGN KEY (spu_id) REFERENCES base_part_spec(id)
  ON DELETE RESTRICT;
