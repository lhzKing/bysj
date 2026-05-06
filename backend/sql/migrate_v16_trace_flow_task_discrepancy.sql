-- B20 task completion discrepancy handling.
-- Adds auditable task-level discrepancy fields used when expected_quantity and
-- actual_quantity differ at completion time.

ALTER TABLE trace_flow_task
  ADD COLUMN discrepancy_type VARCHAR(32) NOT NULL DEFAULT 'NONE'
    COMMENT 'NONE/SHORTAGE/OVERAGE completion discrepancy type' AFTER cancel_time,
  ADD COLUMN discrepancy_quantity INT NOT NULL DEFAULT 0
    COMMENT 'absolute difference between expected and actual quantity' AFTER discrepancy_type,
  ADD COLUMN discrepancy_reason VARCHAR(255) NULL
    COMMENT 'required reason when expected and actual quantity differ' AFTER discrepancy_quantity,
  ADD COLUMN discrepancy_time DATETIME NULL
    COMMENT 'time when discrepancy was recorded' AFTER discrepancy_reason;

ALTER TABLE trace_flow_task
  ADD CONSTRAINT ck_trace_flow_task_discrepancy_type
    CHECK (discrepancy_type IN ('NONE','SHORTAGE','OVERAGE')),
  ADD CONSTRAINT ck_trace_flow_task_discrepancy_quantity
    CHECK (discrepancy_quantity >= 0);
